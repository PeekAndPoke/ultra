package de.peekandpoke.ultra.vault

import kotlinx.serialization.Serializable

object VaultModels {

    @Serializable
    data class RepositoryInfo(
        val connection: String,
        val name: String,
        val stats: RepositoryStats,
        val indexes: IndexesInfo,
    ) {
        val hasErrors get() = indexes.missingIndexes.isNotEmpty() || indexes.excessIndexes.isNotEmpty()
    }

    @Serializable
    data class RepositoryStats(
        val type: String?,
        val isSystem: Boolean?,
        val status: String?,
        val storage: Storage,
        val indexes: Indexes,
        val custom: List<Custom>,
    ) {
        companion object {
            val empty = RepositoryStats(
                type = null,
                isSystem = null,
                status = null,
                storage = Storage(),
                indexes = Indexes(),
                custom = emptyList(),
            )
        }

        @Serializable
        data class Storage(
            val count: Long? = null,
            val avgSize: Long? = null,
            val totalSize: Long? = null,
        )

        @Serializable
        data class Indexes(
            val count: Long? = null,
            val totalSize: Long? = null,
        )

        @Serializable
        data class Custom(
            val name: String,
            val entries: Map<String, String?>,
        ) {
            companion object {
                fun of(name: String, entries: Map<String, Any?>) = Custom(
                    name = name,
                    entries = entries.mapValues { it.value?.toString() },
                )
            }
        }
    }

    @Serializable
    data class IndexesInfo(
        /** Indexes that exist as they are defined */
        val healthyIndexes: List<IndexInfo>,
        /** Indexes that are defined but do not exist */
        val missingIndexes: List<IndexInfo>,
        /** Indexes that exist but are not defined */
        val excessIndexes: List<IndexInfo>,
    ) {
        companion object {
            val empty = IndexesInfo(
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
