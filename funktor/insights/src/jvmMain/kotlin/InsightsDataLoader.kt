package io.peekandpoke.funktor.insights

import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.insights.api.InsightsCollectorSlice
import io.peekandpoke.funktor.insights.api.InsightsRecord
import io.peekandpoke.funktor.insights.api.InsightsRecordSummary
import io.peekandpoke.funktor.insights.collectors.RequestCollector
import io.peekandpoke.funktor.insights.collectors.ResponseCollector
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

        val siblings = repository.listItems(file.parentPath).sortedByDescending { it.lastModifiedAt }
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
     * The [limit] most recent records, newest first.
     *
     * **Provisional shape — see "Blockers collected" in the task file.** Building a summary means
     * opening each record, so the result is bounded rather than paged: day folders are walked
     * newest-first and reading stops as soon as [limit] is reached.
     */
    suspend fun list(limit: Int): List<InsightsRecordSummary> {
        val dayFolders = repository.listItems("")
            .filter { it is DepotItem.Folder && it.name.startsWith(DAY_FOLDER_PREFIX) }
            .sortedByDescending { it.name }

        val result = mutableListOf<InsightsRecordSummary>()

        for (folder in dayFolders) {
            if (result.size >= limit) break

            val files = repository.listItems(folder.path)
                .filterIsInstance<DepotItem.File>()
                .sortedByDescending { it.lastModifiedAt }

            for (file in files) {
                if (result.size >= limit) break

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
     * Derives the list columns from the request and response slices.
     *
     * `RequestCollector.Data` no longer carries a computed `fullUrl`: Jackson emitted it but Slumber
     * (constructor parameters only) would not have, so the stored file and the API would have disagreed
     * silently. The url is rebuilt from its parts here instead.
     */
    private fun JsonObject.summary(path: String, recordedAt: MpInstant?): InsightsRecordSummary {
        val slices = slices().associate { it.key to it.data }
        val request = slices[RequestCollector.Data.KEY]?.obj()
        val response = slices[ResponseCollector.Data.KEY]?.obj()

        val scheme = request?.get("scheme")?.str()
        val host = request?.get("host")?.str()
        val port = request?.get("port")?.num()?.toInt()
        val uri = request?.get("uri")?.str()

        return InsightsRecordSummary(
            path = path,
            recordedAt = recordedAt,
            // ktor's HttpMethod and HttpStatusCode are data classes, so Jackson nests the scalar
            method = request?.get("method")?.unwrap("value")?.str(),
            url = if (scheme != null && host != null && uri != null) "$scheme://$host:$port$uri" else null,
            status = response?.get("status")?.unwrap("value")?.num()?.toInt(),
            durationMs = durationMs(),
        )
    }

    private fun JsonElement.obj(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun JsonElement.str(): String? = runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    private fun JsonElement.num(): Double? =
        (this as? JsonPrimitive)?.let { it.longOrNull?.toDouble() ?: it.doubleOrNull }

    /** Reads [field] out of an object, or returns the element unchanged when it is already a scalar. */
    private fun JsonElement.unwrap(field: String): JsonElement? = obj()?.get(field) ?: this
}
