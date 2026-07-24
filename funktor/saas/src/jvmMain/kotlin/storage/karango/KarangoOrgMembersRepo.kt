package io.peekandpoke.funktor.saas.storage.karango

import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.funktor.saas.domain.OrgMember
import io.peekandpoke.funktor.saas.domain.Organisation
import io.peekandpoke.funktor.saas.domain.org
import io.peekandpoke.funktor.saas.domain.softDelete
import io.peekandpoke.funktor.saas.domain.userId
import io.peekandpoke.funktor.saas.storage.OrgMembersStorage
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.notDeleted
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class KarangoOrgMembersRepo(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : OrgMembersStorage.Vault.Repo, EntityRepository<OrgMember>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    class Fixtures(repo: KarangoOrgMembersRepo) : RepoFixtureLoader<OrgMember>(repo)

    override fun KarangoIndexBuilder<OrgMember>.buildIndexes() {
        // One membership per (org, user). `org` is a Ref, stored as its `_id` string, so it indexes
        // like any string field.
        persistentIndex {
            field { org }
            field { userId }

            options {
                unique(true)
            }
        }

        // `findByUser` (the login / getMemberships path) filters on userId alone, which is NOT a
        // leftmost prefix of the compound index above — so index it on its own to avoid a full scan.
        persistentIndex {
            field { userId }
        }
    }

    override suspend fun findByUser(userId: String): List<Stored<OrgMember>> = find {
        FOR(repo) { member ->
            FILTER(member.userId EQ userId)
            FILTER(notDeleted(member.softDelete))
            RETURN(member)
        }
    }.toList()

    override suspend fun findByOrg(org: Ref<Organisation>): List<Stored<OrgMember>> = find {
        FOR(repo) { member ->
            FILTER(member.org EQ org._id)
            FILTER(notDeleted(member.softDelete))
            RETURN(member)
        }
    }.toList()

    override suspend fun findByOrgAndUser(org: Ref<Organisation>, userId: String): Stored<OrgMember>? = findFirst {
        FOR(repo) { member ->
            FILTER(member.org EQ org._id)
            FILTER(member.userId EQ userId)
            FILTER(notDeleted(member.softDelete))
            LIMIT(1)
            RETURN(member)
        }
    }

    override suspend fun findByOrgAndUserIncludingDeleted(org: Ref<Organisation>, userId: String): Stored<OrgMember>? =
        findFirst {
            // Deliberately NO notDeleted filter — the reactivation path must see the retained slot.
            FOR(repo) { member ->
                FILTER(member.org EQ org._id)
                FILTER(member.userId EQ userId)
                LIMIT(1)
                RETURN(member)
            }
        }
}
