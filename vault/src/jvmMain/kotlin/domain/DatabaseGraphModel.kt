package de.peekandpoke.ultra.vault.domain

data class DatabaseGraphModel(
    val repos: List<Repo>,
) {
    data class Repo(
        val id: Id,
        val connection: String,
        val storedClasses: List<StoredClass>,
    ) {
        data class Id(
            val name: String,
            val connection: String,
        )
    }

    data class StoredClass(
        val fqn: String,
        val references: List<Reference>,
    )

    data class Reference(
        val repo: Repo.Id?,
        val fqn: String,
        val type: Type,
    ) {
        enum class Type {
            Direct,
            Lazy,
        }
    }
}
