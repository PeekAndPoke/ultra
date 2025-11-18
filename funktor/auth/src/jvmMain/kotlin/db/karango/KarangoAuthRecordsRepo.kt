package de.peekandpoke.funktor.auth.db.karango

import de.peekandpoke.funktor.auth.AuthRecordStorage
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.domain.createdAt
import de.peekandpoke.funktor.auth.domain.expiresAt
import de.peekandpoke.funktor.auth.domain.ownerId
import de.peekandpoke.funktor.auth.domain.realm
import de.peekandpoke.funktor.auth.domain.token
import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql._type
import de.peekandpoke.karango.aql.ts
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook

class KarangoAuthRecordsRepo(
    name: String = "system_auth_records",
    driver: KarangoDriver,
    onAfterSaves: List<AuthRecordStorage.OnAfterSave>,
    timestamped: TimestampedHook,
) : AuthRecordStorage.Adapter.Vault.Repo, EntityRepository<AuthRecord>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamped.onBeforeSave())
) {
    class Fixtures(repo: KarangoAuthRecordsRepo) : RepoFixtureLoader<AuthRecord>(repo)

    override fun IndexBuilder<AuthRecord>.buildIndexes() {
        persistentIndex {
            field { realm }
            field { ownerId }
            field { _type }
        }

        ttlIndex {
            field { expiresAt }
        }
    }

    override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.ownerId EQ owner)

                SORT(r.createdAt.ts.DESC)

                LIMIT(1)

                RETURN(r)
            }
        }
    }

    override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
        return findFirst {
            FOR(repo) { r ->
                FILTER(r._type EQ type)
                FILTER(r.realm EQ realm)
                FILTER(r.token EQ token)

                LIMIT(1)

                RETURN(r)
            }
        }
    }
}
