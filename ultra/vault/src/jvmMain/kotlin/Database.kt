package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.SimpleLookup
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

    fun <T : Any> hasRepositoryStoring(type: Class<T>): Boolean {

        return null != repoClassLookup.getOrPut(type) {
            @Suppress("UNCHECKED_CAST")
            repositories.all()
                .firstOrNull { it.stores(type.kotlin) }
                ?.let { it::class.java as Class<Repository<*>> }
        }
    }

    fun <T : Any> getRepositoryStoring(type: Class<T>): Repository<T> {
        return getRepositoryStoringOrNull(type)
        // TODO: use customer exception
            ?: error("No repository stores the type '$type'")
    }

    fun <T : Any> getRepositoryStoringOrNull(type: Class<T>): Repository<T>? {

        val cls = repoClassLookup.getOrPut(type) {
            repositories.all()
                .firstOrNull { it.stores(type.kotlin) }
                ?.let { it::class.java }
        }

        return cls?.let {
            @Suppress("UNCHECKED_CAST")
            getRepository(it) as Repository<T>
        }
    }

    fun <T : Repository<*>> getRepository(cls: KClass<T>): T = getRepository(cls.java)

    fun <T : Repository<*>> getRepository(cls: Class<T>): T {
        return repositories.getOrNull(cls.kotlin)
            ?: throw VaultException("No repository of class '$cls' is registered.")
    }

    inline fun <reified T : Repository<*>> getRepository() = getRepository(T::class.java)

    fun getRepository(name: String): Repository<*> {
        val cls = repoClassLookup.getOrPut(name) {
            repositories.all().firstOrNull { it.name == name }?.let { it::class.java }
        }

        if (cls != null) {
            return getRepository(cls)
        }

        // TODO: use customer exception
        error("No repository with name '$name' was found")
    }
}
