package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.insights.api.InsightsCollectorSlice
import io.peekandpoke.funktor.insights.api.InsightsRecord
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Reads stored insights records from the depot.
 *
 * Records are parsed as raw JSON and never reconstituted into typed collector objects. The previous
 * implementation called `Class.forName(record.cls)` and only afterwards checked the result was an
 * [InsightsCollectorData], so reading a record ran static initializers of whatever class it named.
 * Serving the tree removes that step rather than reordering it — and it is what the API needs anyway,
 * since each frontend tab parses its own slice.
 */
class InsightsDataLoader(
    private val repository: InsightsRepository,
) {
    companion object {
        /** Lenient: stored records are written by Jackson, whose output we read but do not control. */
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Records are written to `records-<date>/<datetime>.json`. */
        private const val DAY_FOLDER_PREFIX = "records-"
    }

    /** Loads the record [ref] addresses, or null when there is no such file. */
    suspend fun load(ref: InsightsRecordRef): InsightsRecord? {
        val path = ref.toPath()

        val content = repository.getContent(path) ?: return null
        val file = repository.getFile(path) ?: return null

        val root = parse(content.getContentBytes()) ?: return null

        // Sorted by NAME, not lastModifiedAt: records are written asynchronously
        // (`launch(Dispatchers.IO) { delay(1) }`), so mtime is WRITE time and reorders under load —
        // measured on real data, five records whose filenames run 872,875,895,905,945 had mtimes that
        // sorted 905,872,875,945,895. The filename is `<LocalDateTime>.json`, which sorts
        // chronologically, needs no stat, and cannot tie.
        val siblings = repository.listItems(file.parentPath).sortedByDescending { it.name }
        val idx = siblings.indexOfFirst { it.path == path }

        return InsightsRecord(
            ref = ref,
            recordedAt = file.lastModifiedAt,
            // The headline is read here too, not only in the list. A BRIEF record has no collectors at
            // all, so without this a detail page opened by deep link showed an empty envelope and no way
            // to tell "recorded briefly" from "recorded nothing".
            method = root["method"]?.str(),
            path = root["uri"]?.str(),
            status = root["status"]?.num()?.toInt(),
            durationMs = root.durationMs(),
            collectors = root.slices(),
            // siblings are newest-first, so the NEXT record in time sits at the LOWER index
            next = if (idx > 0) siblings.getOrNull(idx - 1)?.path?.toRef() else null,
            previous = if (idx >= 0) siblings.getOrNull(idx + 1)?.path?.toRef() else null,
        )
    }

    /**
     * One page of records, newest first.
     *
     * Day folders and the files inside them are ordered by NAME — both encode their timestamp, so the
     * order is chronological without a single `stat` and without depending on write time. Only the
     * files on the requested page are opened; the rest are never read.
     */
    suspend fun list(page: Int, epp: Int): List<InsightsRecordSummary> {
        // Long because `page` is caller-supplied and `(page - 1) * epp` wraps NEGATIVE in Int —
        // ?page=2147483647&epp=200 gave skip = -400.
        //
        // Belt and braces, not the load-bearing part: reverting this to Int does NOT change any result,
        // because the slot-based break below sees `seen - skip` already exceeding `epp` for any
        // large-magnitude negative skip and returns an empty page. Verified by mutation — the Int
        // version fails no test. Kept because it states the intent, and because the protection would
        // vanish if the break condition were ever changed back to counting rows.
        val skip = (page.toLong() - 1) * epp

        val dayFolders = repository.listItems("")
            .filter { it is DepotItem.Folder && it.name.startsWith(DAY_FOLDER_PREFIX) }
            .sortedByDescending { it.name }

        val result = mutableListOf<InsightsRecordSummary>()
        var seen = 0L

        for (folder in dayFolders) {
            if (seen - skip >= epp) break

            val files = repository.listItems(folder.path)
                .filterIsInstance<DepotItem.File>()
                .sortedByDescending { it.name }

            for (file in files) {
                // A page consumes exactly `epp` SLOTS, not `epp` rows. Breaking on `result.size` instead
                // made an unreadable record consume a slot on this page and get skipped again on the
                // next, so the two pages OVERLAPPED — page 1 walked further than `epp` files while page 2
                // still skipped only `page * epp`. Truncated records are routine here: `putFile` is a
                // bare non-atomic `writeBytes` and records are written after the response, so a listing
                // against live traffic reads half-written files.
                if (seen - skip >= epp) break

                val slot = seen++

                // skip cheaply — the file is never opened
                if (slot < skip) continue

                val root = parse(repository.getContent(file.path)?.getContentBytes()) ?: continue

                val ref = file.path.toRef() ?: continue

                result.add(root.summary(ref = ref, recordedAt = file.lastModifiedAt))
            }
        }

        return result
    }

    private fun parse(bytes: ByteArray?): JsonObject? = runCatching {
        json.parseToJsonElement(String(bytes ?: return null)).jsonObject
    }.getOrNull()

    /** Every `{ key, data }` entry, unknown keys included — the envelope stays open. */
    private fun JsonObject.slices(): List<InsightsCollectorSlice> =
        runCatching { this["collectors"]?.jsonArray }.getOrNull()
            ?.mapNotNull { element ->
                val obj = element.obj() ?: return@mapNotNull null
                val key = obj["key"]?.str() ?: return@mapNotNull null

                InsightsCollectorSlice(key = key, data = obj["data"] ?: JsonNull)
            }
            ?: emptyList()

    /** Null when the record carries no timing — which must stay distinct from a genuine `0.0`. */
    private fun JsonObject.durationMs(): Double? {
        val started = this["startedNs"]?.num()?.toLong() ?: return null
        val ended = this["endedNs"]?.num()?.toLong() ?: return null

        return (ended - started) / 1_000_000.0
    }

    /**
     * Reads the list columns straight off the record.
     *
     * The headline is stored at the top level, so this never touches `collectors` — which is what makes
     * listing cheap (the kontainer slice alone averages 137 KB) and what makes a BRIEF record, which
     * has no collectors at all, listable.
     */
    private fun JsonObject.summary(ref: InsightsRecordRef, recordedAt: MpInstant?) = InsightsRecordSummary(
        ref = ref,
        recordedAt = recordedAt,
        method = this["method"]?.str(),
        path = this["uri"]?.str(),
        status = this["status"]?.num()?.toInt(),
        durationMs = durationMs(),
    )

    /** `records-2026-07-31/a.json` -> `(records-2026-07-31, a.json)`; null when it is not two segments. */
    private fun String.toRef(): InsightsRecordRef? {
        val bucket = substringBeforeLast('/', missingDelimiterValue = "")
        val file = substringAfterLast('/', missingDelimiterValue = "")

        return when {
            bucket.isBlank() || file.isBlank() -> null
            else -> InsightsRecordRef(bucket = bucket, file = file)
        }
    }

    private fun JsonElement.obj(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement.str(): String? = runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    private fun JsonElement.num(): Double? =
        (this as? JsonPrimitive)?.let { it.longOrNull?.toDouble() ?: it.doubleOrNull }
}
