package io.peekandpoke.funktor.saas.storage

import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored

/**
 * Storage for [OrgMember]s — the org↔user membership collection, unique per (org, userId).
 *
 * The [Null] object is registered by default; selecting a backend via `funktorSaas { useKarango() }`
 * or `useMonko()` swaps in the [Vault] implementation.
 */
interface OrgMembersStorage {

    /** No-op storage — reads return empty, writes fail loudly. Active until a backend is selected. */
    class Null : OrgMembersStorage {
        override suspend fun findByUser(userId: String): List<Stored<OrgMember>> = emptyList()
        override suspend fun findByOrg(org: Ref<Organisation>): List<Stored<OrgMember>> = emptyList()
        override suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String): Stored<OrgMember>? = null
        override suspend fun add(
            org: Stored<Organisation>,
            userId: String,
            roles: Set<String>,
            branchIds: Set<String>,
        ): Stored<OrgMember> = notConfigured()

        override suspend fun save(member: Storable<OrgMember>): Stored<OrgMember> = notConfigured()
        override suspend fun remove(member: Stored<OrgMember>) { notConfigured() } // a write — fail loud
        override suspend fun clear() { /* noop — mirrors OrgsStorage.Null */ }

        private fun notConfigured(): Nothing =
            error("OrgMembersStorage backend not configured — call useKarango() or useMonko() on funktorSaas { }")
    }

    /** Vault-backed storage, delegating to a backend [Repo] (Karango or Monko). */
    class Vault(private val repo: Repo) : OrgMembersStorage {
        interface Repo : Repository<OrgMember> {
            suspend fun findByUser(userId: String): List<Stored<OrgMember>>
            suspend fun findByOrg(org: Ref<Organisation>): List<Stored<OrgMember>>
            suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String): Stored<OrgMember>?
        }

        override suspend fun findByUser(userId: String) = repo.findByUser(userId)
        override suspend fun findByOrg(org: Ref<Organisation>) = repo.findByOrg(org)
        override suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String) =
            repo.findByOrgAndUser(org, userId)

        override suspend fun save(member: Storable<OrgMember>) = repo.save(member)
        override suspend fun clear() { repo.removeAll() }

        override suspend fun add(
            org: Stored<Organisation>,
            userId: String,
            roles: Set<String>,
            branchIds: Set<String>,
        ): Stored<OrgMember> = repo.insert(
            OrgMember(org = org.asRef, userId = userId, roles = roles, branchIds = branchIds)
        )

        override suspend fun remove(member: Stored<OrgMember>) {
            repo.remove(member._id)
        }
    }

    /** Every organisation the given [userId] belongs to. Backs the realm `getMemberships()` hook. */
    suspend fun findByUser(userId: String): List<Stored<OrgMember>>

    /** Every member of the given [org]. */
    suspend fun findByOrg(org: Ref<Organisation>): List<Stored<OrgMember>>

    /** The single membership of [userId] in [org], or `null`. */
    suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String): Stored<OrgMember>?

    /** Adds a membership row for [userId] in [org] with the given [roles]/[branchIds]. */
    suspend fun add(
        org: Stored<Organisation>,
        userId: String,
        roles: Set<String> = emptySet(),
        branchIds: Set<String> = emptySet(),
    ): Stored<OrgMember>

    suspend fun save(member: Storable<OrgMember>): Stored<OrgMember>
    suspend fun remove(member: Stored<OrgMember>)
    suspend fun clear()
}
