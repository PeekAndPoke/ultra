package de.peekandpoke.funktor.insights

import com.fasterxml.jackson.module.kotlin.readValue
import de.peekandpoke.funktor.insights.gui.InsightsGuiData
import de.peekandpoke.ultra.log.Log
import kotlin.reflect.full.allSuperclasses

class InsightsDataLoader(
    private val repository: InsightsRepository,
    private val mapper: InsightsMapper,
    private val log: Log,
) {
    /**
     * Load the gui data stored in the given [path]
     */
    suspend fun loadGuiData(path: String): InsightsGuiData? {
        // get the actual file
        val content = repository.getContent(path) ?: return null
        val file = repository.getFile(path) ?: return null

        // get all newest files
        val siblings = repository.listItems(file.parentPath).sortedByDescending { it.lastModifiedAt }

        val fileIdx = siblings.indexOfFirst { it.path == path }
        // get the previous and next file
        val nextFile = if (fileIdx > 0) siblings[fileIdx - 1] else null
        val previousFile = if (fileIdx < siblings.size - 1) siblings[fileIdx + 1] else null

        // read file contents
        val recordString = content.getContentBytes()?.let { String(it) } ?: ""
        val insightsData = mapper.readValue<InsightsData>(recordString)

        val collectors = insightsData.collectors
            .mapNotNull {
                try {
                    val cls = Class.forName(it.cls).kotlin

                    if (!cls.allSuperclasses.contains(InsightsCollectorData::class)) {
                        return@mapNotNull null
                    }

                    return@mapNotNull mapper.convertValue(it.data, cls.java) as InsightsCollectorData
                } catch (e: Throwable) {
                    log.warning("Could not deserialize collector ${it.cls} in ${file.path} - ${e.message}")

                    return@mapNotNull null
                }
            }

        return InsightsGuiData(
            ts = insightsData.ts,
            date = insightsData.date,
            startedNs = insightsData.startedNs,
            endedNs = insightsData.endedNs,
            collectors = collectors,
            nextFile = nextFile,
            previousFile = previousFile,
        )
    }
}
