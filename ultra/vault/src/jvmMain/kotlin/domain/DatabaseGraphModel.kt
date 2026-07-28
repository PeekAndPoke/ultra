package io.peekandpoke.ultra.vault.domain

/**
 * Snapshot of all repositories of a database and the references between the classes they store.
 *
 * Produced by `DatabaseGraphBuilder` and consumed by funktor's insights panel.
 */
data class DatabaseGraphModel(
    val repos: List<Repo>,
) {
    /**
     * A repository and the classes it stores.
     *
     * @param connection Duplicates [Id.connection] — both are taken from the same repository.
     */
    data class Repo(
        val id: Id,
        val connection: String,
        val storedClasses: List<StoredClass>,
    ) {
        /** Identifies a repository by its name and the connection it belongs to. */
        data class Id(
            val name: String,
            val connection: String,
        )
    }

    /** A class stored by a repository, given by its fully qualified name, and its references. */
    data class StoredClass(
        val fqn: String,
        val references: List<Reference>,
    )

    /**
     * A reference from a stored class to another entity type.
     *
     * @param repo The repository storing the referenced type, or `null` when no registered
     *   repository stores it — the reference then leaves the known graph.
     * @param fqn Fully qualified name of the referenced type.
     */
    data class Reference(
        val repo: Repo.Id?,
        val fqn: String,
        val type: Type,
    ) {
        /**
         * Kind of a reference.
         *
         * Obsolete: `LazyRef` was merged into `Ref`, so every reference is lazy and all of them
         * are emitted as [Direct].
         */
        enum class Type {
            /** The only kind that is ever produced. */
            Direct,

            /** Dead — nothing constructs this any more. */
            Lazy,
        }
    }
}
