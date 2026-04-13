package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.common.SimpleLookup
import kotlin.reflect.KClass

class Database(
    private val repositories: Lookup<Repository<*>>,
    private val repoClassLookup: SharedRepoClassLookup,
) {
    companion object {
        val withNoRepos = Database(
            repositories = SimpleLookup { emptyList() },
            repoClassLookup = SharedRepoClassLookup(),
        )

        fun of(repos: () -> List<Repository<*>>) = Database(
            repositories = SimpleLookup(repos),
            repoClassLookup = SharedRepoClassLookup(),
        )
    }

    fun getRepositories(): List<Repository<*>> {
        return repositories.all()
    }

    suspend fun ensureRepositories() {
        repositories.all().forEach { repo ->
            repo.ensure()
        }
    }

    suspend fun validateIndexes(): List<VaultModels.IndexesInfo> {
        return repositories.all().map { repo ->
            repo.validateIndexes()
        }
    }

    suspend fun ensureIndexes() {
        repositories.all().forEach { repo ->
            repo.ensureIndexes()
            repo.validateIndexes()
        }
    }

    suspend fun recreateIndexes() {
        repositories.all().forEach { repo ->
            repo.recreateIndexes()
            repo.validateIndexes()
        }
    }

    fun <T : Any> hasRepositoryStoring(type: KClass<T>): Boolean {
        return getRepositoryStoringOrNull(type) != null
    }

    fun <T : Any> getRepositoryStoring(type: KClass<T>): Repository<T> {
        return getRepositoryStoringOrNull(type)
            ?: throw VaultException("No repository stores the type '$type'")
    }

    fun <T : Any> getRepositoryStoringOrNull(type: KClass<T>): Repository<T>? {

        val cls = repoClassLookup.getOrPut(type) {
            repositories.all()
                .firstOrNull { it.stores(type) }
                ?.let { it::class }
        }

        return cls?.let {
            @Suppress("UNCHECKED_CAST")
            getRepository(it) as Repository<T>
        }
    }

    fun <T : Repository<*>> getRepository(cls: KClass<T>): T {
        return repositories.getOrNull(cls)
            ?: throw VaultException("No repository of class '$cls' is registered.")
    }

    inline fun <reified T : Repository<*>> getRepository() = getRepository(T::class)

    fun getRepository(name: String): Repository<*> {
        val cls = repoClassLookup.getOrPut(name) {
            repositories.all().firstOrNull { it.name == name }?.let { it::class }
        }

        if (cls != null) {
            return getRepository(cls)
        }

        throw VaultException("No repository with name '$name' was found")
    }
}
