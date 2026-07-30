package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.common.SimpleLookup
import kotlin.reflect.KClass

/**
 * Registry of all [Repository] instances, and the entry point for looking them up.
 *
 * Repositories can be found by their own class, by their [Repository.name] or by the entity type
 * they store. The latter two searches walk the full repository list, so their results are cached in
 * [repoClassLookup] — a kontainer singleton, while [Database] itself is dynamic (see [Ultra_Vault]).
 */
class Database(
    private val repositories: Lookup<Repository<*>>,
    private val repoClassLookup: SharedRepoClassLookup,
) {
    companion object {
        /** A database without any repositories, e.g. as a placeholder in tests. */
        val withNoRepos = Database(
            repositories = SimpleLookup { emptyList() },
            repoClassLookup = SharedRepoClassLookup(),
        )

        /**
         * Creates a standalone database outside of a kontainer, e.g. for tests.
         *
         * [repos] is evaluated lazily on the first lookup and the result is kept, so repositories
         * added afterwards are not picked up.
         */
        fun of(repos: () -> List<Repository<*>>) = Database(
            repositories = SimpleLookup(repos),
            repoClassLookup = SharedRepoClassLookup(),
        )
    }

    /** All registered repositories. */
    fun getRepositories(): List<Repository<*>> {
        return repositories.all()
    }

    /** Creates every repository in the database and sets up its indexes. */
    suspend fun ensureRepositories() {
        repositories.all().forEach { repo ->
            repo.ensure()
        }
    }

    /** Reports missing and excess indexes per repository, without changing anything. */
    suspend fun validateIndexes(): List<VaultModels.IndexesInfo> {
        return repositories.all().map { repo ->
            repo.validateIndexes()
        }
    }

    /**
     * Brings indexes in line with their definitions, then reports the state as [validateIndexes] would.
     *
     * Missing indexes are created and changed ones are dropped and re-created — this is not a purely
     * additive operation. Indexes that are not defined at all are left untouched, so `excessIndexes`
     * in the result is informational here; only [recreateIndexes] clears those.
     */
    suspend fun ensureIndexes(): List<VaultModels.IndexesInfo> {
        repositories.all().forEach { repo ->
            repo.ensureIndexes()
        }

        return validateIndexes()
    }

    /** Drops and re-creates all indexes and reports the resulting state, as [validateIndexes] would. */
    suspend fun recreateIndexes(): List<VaultModels.IndexesInfo> {
        repositories.all().forEach { repo ->
            repo.recreateIndexes()
        }

        return validateIndexes()
    }

    /** Checks whether any repository stores the given [type]. */
    fun <T : Any> hasRepositoryStoring(type: KClass<T>): Boolean {
        return getRepositoryStoringOrNull(type) != null
    }

    /** Gets the repository storing the given [type], or throws a [VaultException]. */
    fun <T : Any> getRepositoryStoring(type: KClass<T>): Repository<T> {
        return getRepositoryStoringOrNull(type)
            ?: throw VaultException("No repository stores the type '$type'")
    }

    /**
     * Gets the first repository whose [Repository.stores] accepts [type], or null.
     *
     * Both hits and misses are cached, so a type that resolved to nothing once keeps resolving to
     * nothing for the lifetime of [repoClassLookup].
     */
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

    /**
     * Gets the repository registered under exactly [cls], or throws a [VaultException].
     *
     * The match is on the runtime class, not on subtypes — a super class of a registered repository
     * does not resolve.
     */
    fun <T : Repository<*>> getRepository(cls: KClass<T>): T {
        return repositories.getOrNull(cls)
            ?: throw VaultException("No repository of class '$cls' is registered.")
    }

    /** Gets the repository registered under exactly [T], or throws a [VaultException]. */
    inline fun <reified T : Repository<*>> getRepository() = getRepository(T::class)

    /** Gets the repository with the given [Repository.name], or throws a [VaultException]. */
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
