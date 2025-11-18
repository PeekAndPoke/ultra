package de.peekandpoke.funktor.auth.db.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import de.peekandpoke.funktor.auth.AuthRecordStorage
import de.peekandpoke.funktor.auth.domain.AuthRecord
import de.peekandpoke.funktor.auth.domain.createdAt
import de.peekandpoke.funktor.auth.domain.ownerId
import de.peekandpoke.funktor.auth.domain.realm
import de.peekandpoke.funktor.auth.domain.token
import de.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import de.peekandpoke.monko.MonkoDriver
import de.peekandpoke.monko.MonkoRepository
import de.peekandpoke.monko.lang._type
import de.peekandpoke.monko.lang.ts
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
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
