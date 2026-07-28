package io.peekandpoke.ultra.vault

import kotlinx.serialization.Serializable

/**
 * Report models describing the state of repositories, shared between server and client.
 *
 * These are produced on demand by the drivers and never persisted — they exist to be rendered in
 * admin tooling.
 */
object VaultModels {

    /** One repository with its figures and the health of its indexes. */
    @Serializable
    data class RepositoryInfo(
        val connection: String,
        val name: String,
        val stats: RepositoryStats,
        val indexes: IndexesInfo,
    ) {
        /**
         * True when the indexes deviate from their definitions in either direction.
         *
         * Computed, so it is not part of the serialized form — a client derives it from [indexes].
         * Note that excess indexes count as an error although `ensureIndexes` never removes them.
         */
        val hasErrors get() = indexes.missingIndexes.isNotEmpty() || indexes.excessIndexes.isNotEmpty()
    }

    /**
     * Figures about a repository as reported by its driver.
     *
     * Everything is nullable because each driver reports a different subset; `empty` is the
     * "nothing known" default used by drivers that do not implement stats at all.
     */
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
            /** Stats with nothing reported at all. */
            val empty = RepositoryStats(
                type = null,
                isSystem = null,
                status = null,
                storage = Storage(),
                indexes = Indexes(),
                custom = emptyList(),
            )
        }

        /** Document count and size of the stored documents, in bytes. */
        @Serializable
        data class Storage(
            val count: Long? = null,
            val avgSize: Long? = null,
            val totalSize: Long? = null,
        )

        /** Number of indexes and the space they take up, in bytes. */
        @Serializable
        data class Indexes(
            val count: Long? = null,
            val totalSize: Long? = null,
        )

        /** A named block of driver-specific figures, already rendered as text for display. */
        @Serializable
        data class Custom(
            val name: String,
            val entries: Map<String, String?>,
        ) {
            companion object {
                /** Builds a block, rendering every value with `toString` and keeping nulls as null. */
                fun of(name: String, entries: Map<String, Any?>) = Custom(
                    name = name,
                    entries = entries.mapValues { it.value?.toString() },
                )
            }
        }
    }

    /**
     * The result of comparing a repository's index definitions against the indexes that exist.
     *
     * Carries no repository identity, so a list of these is only attributable via the order of
     * `Database.getRepositories()`.
     */
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
            /** Nothing defined and nothing found — the default for drivers without index support. */
            val empty = IndexesInfo(
                healthyIndexes = emptyList(),
                missingIndexes = emptyList(),
                excessIndexes = emptyList(),
            )
        }
    }

    /** A single index by name, driver-specific [type] and the field paths it covers. */
    @Serializable
    data class IndexInfo(
        val name: String,
        val type: String,
        val fields: List<String>,
    )
}
