package io.peekandpoke.funktor.auth.db.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
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
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.dsl.eq
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

        // The expiry sweep: markers of one realm by age.
        persistentIndex {
            field { it.realm }
            field { it._type }
            field { it.createdAt.ts }
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
        realm: RealmId, type: String, before: MpInstant, orClaimedBy: String?, after: MpInstant?, limit: Int,
    ): List<Stored<AuthRecord>> {
        val createdAt = repoExpr.createdAt.ts.toFieldPath()
        val aged = Filters.lt(createdAt, before.toEpochMillis())

        val filter = Filters.and(
            listOfNotNull(
                Filters.eq(repoExpr._type.toFieldPath(), type),
                Filters.eq(repoExpr.realm.toFieldPath(), realm.value),
                // `claimedBy` lives on one record type only, so it is addressed by name.
                orClaimedBy?.let { Filters.or(aged, Filters.eq("claimedBy", it)) } ?: aged,
                after?.let { Filters.gt(createdAt, it.toEpochMillis()) },
            )
        )

        val cursor = find { _ ->
            filter(filter)
            sort(Sorts.ascending(createdAt))
            limit(limit)
        }
        return cursor.toList()
    }

    override suspend fun <T : AuthRecord> replaceIfClaimedBy(stored: Stored<T>, expected: String?): Stored<T>? {
        // As the sealed AuthRecord, so the document carries its `_type` exactly like an inserted one.
        @Suppress("UNCHECKED_CAST")
        val document = Document(driver.codec.slumber(kType<AuthRecord>().type, stored.resolve()) as Map<String, Any?>)
        val bsonId = bsonIdOf(stored._key)
        document["_id"] = bsonId

        val result = driver.database.getCollection<Document>(name)
            .replaceOne(claimedBy(bsonId, expected), document, ReplaceOptions().upsert(false))

        return stored.takeIf { result.matchedCount == 1L }
    }

    override suspend fun removeIfClaimedBy(id: String, expected: String?): RemoveResult {
        val result = driver.database.getCollection<Document>(name)
            .deleteOne(claimedBy(bsonIdOf(id.substringAfterLast("/")), expected))

        return RemoveResult(count = result.deletedCount, query = null)
    }

    /** The document [bsonId] while its `claimedBy` equals [expected]; `eq(null)` also matches a missing field. */
    private fun claimedBy(bsonId: Any, expected: String?) =
        Filters.and(Filters.eq("_id", bsonId), Filters.eq("claimedBy", expected))

    /** The same key mapping as removeAllByOwner: an ObjectId when the key is one, else the raw string. */
    private fun bsonIdOf(key: String): Any = if (ObjectId.isValid(key)) ObjectId(key) else key
}
