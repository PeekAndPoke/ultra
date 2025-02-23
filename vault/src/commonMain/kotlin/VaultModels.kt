package de.peekandpoke.ultra.vault

import kotlinx.serialization.Serializable

object VaultModels {

    @Serializable
    data class RepositoryInfo(
        val connection: String,
        val name: String,
        val figures: RepositoryStats,
        val indexes: IndexesInfo,
    ) {
        val hasErrors get() = indexes.missingIndexes.isNotEmpty() || indexes.excessIndexes.isNotEmpty()
    }

    @Serializable
    data class RepositoryStats(
        val id: String?,
        val name: String?,
        val type: String?,
        val isSystem: Boolean?,
        val status: String?,
        val waitForSync: Boolean?,
        val writeConcern: String?,
        val cacheEnabled: Boolean?,
        val cacheInUse: Boolean?,
        val cacheSize: Int?,
        val cacheUsage: Int?,
        val documentCount: Long?,
        val documentsSize: Long?,
        val indexCount: Long?,
        val indexesSize: Long?,
    ) {
        companion object {
            val empty = RepositoryStats(
                id = null,
                name = null,
                type = null,
                isSystem = null,
                status = null,
                waitForSync = null,
                writeConcern = null,
                cacheEnabled = null,
                cacheInUse = null,
                cacheSize = null,
                cacheUsage = null,
                documentCount = null,
                documentsSize = null,
                indexCount = null,
                indexesSize = null,
            )
        }

        val avgSize: Long? = run {
            documentCount?.takeIf { it > 0 }?.let { documentsSize?.div(it) }
        }
    }

    @Serializable
    data class IndexesInfo(
        val connection: String,
        val repository: String,
        /** Indexes that exist as they are defined */
        val healthyIndexes: List<IndexInfo>,
        /** Indexes that are defined but do not exist */
        val missingIndexes: List<IndexInfo>,
        /** Indexes that exist but are not defined */
        val excessIndexes: List<IndexInfo>,
    ) {
        companion object {
            val empty = IndexesInfo(
                connection = "",
                repository = "",
                healthyIndexes = emptyList(),
                missingIndexes = emptyList(),
                excessIndexes = emptyList(),
            )
        }
    }

    @Serializable
    data class IndexInfo(
        val name: String,
        val type: String,
        val fields: List<String>,
    )
}
