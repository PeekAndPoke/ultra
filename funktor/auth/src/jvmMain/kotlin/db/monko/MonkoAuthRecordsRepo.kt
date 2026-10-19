package io.peekandpoke.funktor.auth.db.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import io.peekandpoke.funktor.auth.AuthRecordStorage
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.funktor.auth.domain.createdAt
import io.peekandpoke.funktor.auth.domain.expiresAt
import io.peekandpoke.funktor.auth.domain.ownerId
import io.peekandpoke.funktor.auth.domain.realm
import io.peekandpoke.funktor.auth.domain.token
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.core.fixtures.RepoFixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang._type
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.asc
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.gt
import io.peekandpoke.monko.lang.dsl.lt
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.firstOrNull
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import org.bson.Document
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

        // Meant to mirror Karango's TTL index, but it culls NOTHING today: `expiresAt` is a Long of epoch
        // seconds and a MongoDB TTL index only expires BSON Date fields. The code-level `hasNotExpired()`
        // filter in AuthRecordStorage is therefore the ONLY expiry boundary on Mongo, and expired rows
        // accumulate. Tracked in the parallel plan's backlog (`e4076e46`).
        ttlIndex {
            field { it.expiresAt }
            expireAfter(0)
        }
    }

    override suspend fun findLatest(realm: RealmId, type: String, owner: UserId): Stored<AuthRecord>? {
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

    override suspend fun findByToken(realm: RealmId, type: String, token: String): Stored<AuthRecord>? {
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
        realm: RealmId, type: String, owner: UserId,
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
        realm: RealmId, type: String, owner: UserId, exceptId: String?,
    ): RemoveResult {
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        val filters = mutableListOf(
            Filters.eq(repoExpr._type.toFieldPath(), type),
            Filters.eq(repoExpr.realm.toFieldPath(), realm.value),
            Filters.eq(repoExpr.ownerId.toFieldPath(), owner.value),
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

    override suspend fun removeAllOfOwner(realm: RealmId, owner: UserId, exceptType: String?): RemoveResult {
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        val filters = mutableListOf(
            Filters.eq(repoExpr.realm.toFieldPath(), realm.value),
            Filters.eq(repoExpr.ownerId.toFieldPath(), owner.value),
        )
        if (exceptType != null) {
            filters.add(Filters.ne(repoExpr._type.toFieldPath(), exceptType))
        }

        val result = coll.deleteMany(Filters.and(filters))
        return RemoveResult(count = result.deletedCount, query = null)
    }

    override suspend fun findCreatedBefore(
        realm: RealmId, type: String, before: MpInstant, after: MpInstant?, limit: Int,
    ): List<Stored<AuthRecord>> {
        val cursor = find { r ->
            filter(
                and(
                    listOfNotNull(
                        r._type eq type,
                        r.realm eq realm,
                        r.createdAt.ts lt before.toEpochMillis(),
                        after?.let { r.createdAt.ts gt it.toEpochMillis() },
                    )
                )
            )
            sort(r.createdAt.ts.asc)
            limit(limit)
        }
        return cursor.toList()
    }

    override suspend fun <T : AuthRecord> replaceIfExists(stored: Stored<T>): Stored<T>? {
        @Suppress("UNCHECKED_CAST")
        // As the sealed AuthRecord, so the document carries its `_type` exactly like an inserted one.
        val document = Document(driver.codec.slumber(kType<AuthRecord>().type, stored.resolve()) as Map<String, Any?>)
        // The same key mapping as removeAllByOwner: an ObjectId when the key is one, else the raw string.
        val key = stored._key
        val bsonId: Any = if (ObjectId.isValid(key)) ObjectId(key) else key
        document["_id"] = bsonId

        val result = driver.database.getCollection<Document>(name)
            .replaceOne(Filters.eq("_id", bsonId), document, ReplaceOptions().upsert(false))

        return stored.takeIf { result.matchedCount == 1L }
    }
}
