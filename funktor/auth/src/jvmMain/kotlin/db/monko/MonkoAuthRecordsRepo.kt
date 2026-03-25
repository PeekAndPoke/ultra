package io.peekandpoke.funktor.auth.db.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.ownerId
import io.peekandpoke.funktor.auth.domain.realm
import io.peekandpoke.funktor.auth.domain.token
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang._type
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import org.bson.Document

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

    override suspend fun ensureIndexes() {
        driver.createIndex(
            collection = name,
            keys = Document(
                mapOf(
                    field { it.realm } to 1,
                    field { it.ownerId } to 1,
                    field { it._type } to 1,
                )
            ),
        )
    }

    override suspend fun findLatest(realm: String, type: String, owner: String): Stored<AuthRecord>? {
        val found = find {
            filter(
                Filters.and(
                    Filters.eq(field { it._type }, type),
                    Filters.eq(field { it.realm }, realm),
                    Filters.eq(field { it.ownerId }, owner),
                )
            )

            sort(
                Sorts.descending(field { it.createdAt.ts })
            )

            limit(1)
        }

        return found.firstOrNull()
    }

    override suspend fun findByToken(realm: String, type: String, token: String): Stored<AuthRecord>? {
        val found = find {
            filter(
                Filters.and(
                    Filters.eq(field { it._type }, type),
                    Filters.eq(field { it.realm }, realm),
                    Filters.eq(field { it.token }, token),
                )
            )

            limit(1)
        }

        return found.firstOrNull()
    }
}
