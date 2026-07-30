package io.peekandpoke.funktor.insights

import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Reads stored insights records from the depot.
 *
 * Collector slices come back as the raw tree keyed by [CollectorData.key] — deliberately not
 * reconstituted into typed objects. The previous implementation called `Class.forName(it.cls)` and only
 * then checked the result was an [InsightsCollectorData], so reading a record ran static initializers of
 * any class it named. Serving the raw tree removes that step rather than reordering it, and it is what
 * the API needs anyway: the frontend parses each slice against its own generated schema.
 */
class InsightsDataLoader(
    private val repository: InsightsRepository,
    private val mapper: InsightsMapper,
) {
    /** One stored record, with its neighbours for prev/next navigation. */
    data class Record(
        val path: String,
        val data: InsightsData,
        val nextPath: String?,
        val previousPath: String?,
    )

    /** Loads the record at [path], or null when there is no such file. */
    suspend fun load(path: String): Record? {
        val content = repository.getContent(path) ?: return null
        val file = repository.getFile(path) ?: return null

        val siblings = repository.listItems(file.parentPath).sortedByDescending { it.lastModifiedAt }
        val idx = siblings.indexOfFirst { it.path == path }

        val recordString = content.getContentBytes()?.let { String(it) } ?: return null

        return Record(
            path = path,
            data = mapper.readValue<InsightsData>(recordString),
            // siblings are newest-first, so the NEXT record in time is the PREVIOUS index
            nextPath = if (idx > 0) siblings[idx - 1].path else null,
            previousPath = if (idx in 0 until siblings.size - 1) siblings[idx + 1].path else null,
        )
    }
}
