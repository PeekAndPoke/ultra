package io.peekandpoke.funktor.auth.db.monko

import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.ownerId
import io.peekandpoke.funktor.auth.domain.realm
import io.peekandpoke.funktor.auth.domain.token
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang._type
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class MonkoAuthRecordsRepo(
    name: String = "system_auth_records",
    driver: MonkoDriver,
    onAfterSaves: List<AuthRecordStorage.OnAfterSave>,
    timestamped: TimestampedHook,
) : AuthRecordStorage.Adapter.Vault.Repo, MonkoRepository<AuthRecord>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks
        .of(onAfterSaves)
        .plus(timestamped.onBeforeSave())
) {
    class Fixtures(repo: MonkoAuthRecordsRepo) : RepoFixtureLoader<AuthRecord>(repo)

    override fun MonkoIndexBuilder<AuthRecord>.buildIndexes() {
        persistentIndex {
            field { it.realm }
            field { it.ownerId }
            field { it._type }
        }
    }

    override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
        val found = find { r ->
            filter(
                and(
                    r._type eq type,
                    r.realm eq realm,
                    r.ownerId eq owner,
                )
            )
            sort(r.createdAt.ts.desc)
            limit(1)
        }

        return found.firstOrNull()
    }

    override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
        val found = find { r ->
            filter(
                and(
                    r._type eq type,
                    r.realm eq realm,
                    r.token eq token,
                )
            )
            limit(1)
        }

        return found.firstOrNull()
    }
}
