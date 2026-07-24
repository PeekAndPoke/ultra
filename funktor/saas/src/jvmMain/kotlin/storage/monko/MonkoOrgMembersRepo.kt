package io.peekandpoke.funktor.saas.storage.monko

import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.domain.org
import io.peekandpoke.funktor.saas.domain.softDelete
import io.peekandpoke.funktor.saas.domain.userId
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.notDeleted
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.firstOrNull
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class MonkoOrgMembersRepo(
    name: String = "system_org_members",
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : OrgMembersStorage.Vault.Repo, MonkoRepository<OrgMember>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    class Fixtures(repo: MonkoOrgMembersRepo) : RepoFixtureLoader<OrgMember>(repo)

    override fun MonkoIndexBuilder<OrgMember>.buildIndexes() {
        // One membership per (org, user).
        uniqueIndex {
            field { it.org }
            field { it.userId }
        }

        // `findByUser` (the login / getMemberships path) filters on userId alone, which is NOT a
        // leftmost prefix of the compound index above — so index it on its own to avoid a full scan.
        persistentIndex {
            field { it.userId }
        }
    }

    override suspend fun findByUser(userId: String): List<Stored<OrgMember>> =
        find { r -> filter(and(r.userId eq userId, notDeleted(r.softDelete))) }.toList()

    override suspend fun findByOrg(org: Ref<Organisation>): List<Stored<OrgMember>> =
        find { r -> filter(and(r.org eq org._id, notDeleted(r.softDelete))) }.toList()

    override suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String): Stored<OrgMember>? {
        val found = find { r ->
            filter(
                and(
                    r.org eq org._id,
                    r.userId eq userId,
                    notDeleted(r.softDelete),
                )
            )
            limit(1)
        }
        return found.firstOrNull()
    }
}
