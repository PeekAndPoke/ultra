package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.insights.api.InsightsCollectorSlice
import io.peekandpoke.funktor.insights.api.InsightsRecord
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

    /** Loads the record at [path], or null when there is no such file. */
    suspend fun load(path: String): InsightsRecord? {
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
            path = path,
            recordedAt = file.lastModifiedAt,
            durationMs = root.durationMs(),
            collectors = root.slices(),
            // siblings are newest-first, so the NEXT record in time sits at the LOWER index
            nextPath = if (idx > 0) siblings.getOrNull(idx - 1)?.path else null,
            previousPath = if (idx >= 0) siblings.getOrNull(idx + 1)?.path else null,
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
        val skip = (page - 1) * epp

        val dayFolders = repository.listItems("")
            .filter { it is DepotItem.Folder && it.name.startsWith(DAY_FOLDER_PREFIX) }
            .sortedByDescending { it.name }

        val result = mutableListOf<InsightsRecordSummary>()
        var seen = 0

        for (folder in dayFolders) {
            if (result.size >= epp) break

            val files = repository.listItems(folder.path)
                .filterIsInstance<DepotItem.File>()
                .sortedByDescending { it.name }

            for (file in files) {
                if (result.size >= epp) break

                // skip cheaply — the file is never opened
                if (seen++ < skip) continue

                val root = parse(repository.getContent(file.path)?.getContentBytes()) ?: continue

                result.add(root.summary(path = file.path, recordedAt = file.lastModifiedAt))
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

    private fun JsonObject.durationMs(): Double {
        val started = this["startedNs"]?.num()?.toLong() ?: return 0.0
        val ended = this["endedNs"]?.num()?.toLong() ?: return 0.0

        return (ended - started) / 1_000_000.0
    }

    /**
     * Reads the list columns straight off the record.
     *
     * The headline is stored at the top level, so this never touches `collectors` — which is what makes
     * listing cheap (the kontainer slice alone averages 137 KB) and what makes a BRIEF record, which
     * has no collectors at all, listable.
     */
    private fun JsonObject.summary(path: String, recordedAt: MpInstant?) = InsightsRecordSummary(
        path = path,
        recordedAt = recordedAt,
        method = this["method"]?.str(),
        url = this["uri"]?.str(),
        status = this["status"]?.num()?.toInt(),
        durationMs = durationMs(),
    )

    private fun JsonElement.obj(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement.str(): String? = runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    private fun JsonElement.num(): Double? =
        (this as? JsonPrimitive)?.let { it.longOrNull?.toDouble() ?: it.doubleOrNull }
}
