package io.peekandpoke.funktor.auth.db.monko

import com.mongodb.client.model.Filters
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.expiresAt
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
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.firstOrNull
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import org.bson.types.ObjectId

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

        // Mirror Karango's TTL index so expired auth records (sessions, password-recovery
        // tokens, email-verification tokens, email-change tokens) are culled automatically.
        // The code-level `hasNotExpired()` filter in AuthRecordStorage is then a safety net
        // rather than the only boundary.
        ttlIndex {
            field { it.expiresAt }
            expireAfter(0)
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

    override suspend fun findAllByOwner(
        realm: String, type: String, owner: String,
    ): List<Stored<AuthRecord>> {
        val cursor = find { r ->
            filter(
                and(
                    r._type eq type,
                    r.realm eq realm,
                    r.ownerId eq owner,
                )
            )
            sort(r.createdAt.ts.desc)
        }
        return cursor.toList()
    }

    override suspend fun removeAllByOwner(
        realm: String, type: String, owner: String, exceptId: String?,
    ): RemoveResult {
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        val filters = mutableListOf(
            Filters.eq(repoExpr._type.toFieldPath(), type),
            Filters.eq(repoExpr.realm.toFieldPath(), realm),
            Filters.eq(repoExpr.ownerId.toFieldPath(), owner),
        )
        if (exceptId != null) {
            // Stored._id is formatted "$collection/$stringKey"; the Mongo document's _id is
            // either an ObjectId (auto-generated) or the raw string key. substringAfterLast
            // rather than substringAfter so collection names containing '/' don't break the
            // split. Try both ObjectId + raw string for the _id filter.
            val key = exceptId.substringAfterLast("/")
            val idValue: Any = if (ObjectId.isValid(key)) ObjectId(key) else key
            filters.add(Filters.ne("_id", idValue))
        }

        val result = coll.deleteMany(Filters.and(filters))
        return RemoveResult(count = result.deletedCount, query = null)
    }
}
