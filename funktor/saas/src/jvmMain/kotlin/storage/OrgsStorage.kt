package io.peekandpoke.funktor.saas.storage

import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored

/**
 * Storage for [Organisation]s.
 *
 * The [Null] object is registered by default; selecting a backend via `funktorSaas { useKarango() }`
 * or `useMonko()` swaps in the [Vault] implementation.
 */
interface OrgsStorage {

    /** No-op storage — reads return empty, writes fail loudly. Active until a backend is selected. */
    class Null : OrgsStorage {
        override suspend fun findAll(): List<Stored<Organisation>> = emptyList()
        override suspend fun findById(id: String): Stored<Organisation>? = null
        override suspend fun findBySlug(slug: String): Stored<Organisation>? = null
        override suspend fun create(organisation: Organisation): Stored<Organisation> = notConfigured()
        override suspend fun ensureBySlug(slug: String, name: String): Stored<Organisation> = notConfigured()
        override suspend fun save(organisation: Stored<Organisation>): Stored<Organisation> = notConfigured()
        override suspend fun clear() { /* noop */ }

        private fun notConfigured(): Nothing =
            error("OrgsStorage backend not configured — call useKarango() or useMonko() on funktorSaas { }")
    }

    /** Vault-backed storage, delegating to a backend [Repo] (Karango or Monko). */
    class Vault(private val repo: Repo) : OrgsStorage {
        interface Repo : Repository<Organisation> {
            suspend fun findBySlug(slug: String): Stored<Organisation>?
        }

        override suspend fun findAll(): List<Stored<Organisation>> = repo.findAll().toList()
        override suspend fun findById(id: String): Stored<Organisation>? = repo.findById(id)
        override suspend fun findBySlug(slug: String): Stored<Organisation>? = repo.findBySlug(slug)
        override suspend fun create(organisation: Organisation): Stored<Organisation> = repo.insert(organisation)
        override suspend fun save(organisation: Stored<Organisation>): Stored<Organisation> = repo.save(organisation)
        override suspend fun clear() { repo.removeAll() }

        override suspend fun ensureBySlug(slug: String, name: String): Stored<Organisation> {
            repo.findBySlug(slug)?.let { return it }

            return try {
                repo.insert(Organisation(slug = slug, name = name))
            } catch (ex: Exception) {
                // Lost a create race against another caller/JVM — the unique slug index rejected the
                // insert. The winner's row now exists, so re-read it; rethrow if it genuinely isn't there.
                repo.findBySlug(slug) ?: throw ex
            }
        }
    }

    suspend fun findAll(): List<Stored<Organisation>>
    suspend fun findById(id: String): Stored<Organisation>?
    suspend fun findBySlug(slug: String): Stored<Organisation>?
    suspend fun create(organisation: Organisation): Stored<Organisation>

    /**
     * Idempotently ensures an organisation with the given [slug] exists, creating it with [name] if
     * absent. Returns the existing or newly-created organisation. Safe to call on every startup.
     */
    suspend fun ensureBySlug(slug: String, name: String): Stored<Organisation>
    suspend fun save(organisation: Stored<Organisation>): Stored<Organisation>
    suspend fun clear()
}
