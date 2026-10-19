package io.peekandpoke.funktor.cluster.locks.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.peekandpoke.funktor.cluster.locks.VaultGlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.created
import io.peekandpoke.funktor.cluster.locks.domain.expires
import io.peekandpoke.funktor.cluster.locks.domain.isLegacy
import io.peekandpoke.funktor.cluster.locks.domain.lastHeartbeat
import io.peekandpoke.funktor.cluster.locks.domain.token
import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.combine
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.ts
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.ensureKey
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.bson.conversions.Bson

class MonkoGlobalLocksRepo(driver: MonkoDriver, repoName: String) :
    MonkoRepository<GlobalLockEntry>(name = repoName, kType(), driver), VaultGlobalLocksProvider.Repo {

    class Fixtures(repo: Lazy<MonkoGlobalLocksRepo>) : FixtureLoader {

        private val repo: MonkoGlobalLocksRepo by repo

        override suspend fun prepare(result: FixtureLoader.MutableResult) {
            result.info("Clearing repo ${repo.name}")
            repo.removeAll()
        }

        override suspend fun load(result: FixtureLoader.MutableResult) {
            // noop
        }
    }

    private val collection get() = driver.database.getCollection<Document>(name)

    // The default lookup turns a key that looks like an ObjectId into one, and would then miss the lock.
    override suspend fun findById(id: String?): Stored<GlobalLockEntry>? {
        val key = id?.ensureKey ?: return null

        return find {
            filter(byKey(key))
            limit(1)
        }.asFlow().firstOrNull()
    }

    override suspend fun heartbeat(key: String, token: String, heartbeatAt: MpInstant, expiresAt: MpInstant): Boolean {
        // Update values bypass slumber. Slumbered here, so that the codec reading them back also decides their shape,
        // not the driver's own codec for MpInstant (bson-kotlinx, which today happens to write the same shape).
        val result = collection.updateOne(
            heldWith(key = key, token = token),
            combine(
                Updates.set(repoExpr.lastHeartbeat.toFieldPath(), driver.codec.slumber(heartbeatAt)),
                Updates.set(repoExpr.expires.toFieldPath(), driver.codec.slumber(expiresAt)),
            ),
        )

        return result.matchedCount > 0
    }

    override suspend fun removeIfHeld(key: String, token: String): Boolean {
        return collection.deleteOne(heldWith(key = key, token = token)).deletedCount > 0
    }

    override suspend fun removeIfUnchanged(entry: GlobalLockEntry): Boolean {
        val unchanged = when (entry.isLegacy) {
            // Written without heartbeats: the creation time tells two acquisitions apart.
            true -> repoExpr.created.ts eq entry.created.toEpochMillis()
            // The same acquisition, and no heartbeat since it was read.
            false -> and(
                repoExpr.token eq entry.token,
                repoExpr.lastHeartbeat.ts eq entry.lastHeartbeat.toEpochMillis(),
            )
        }

        return collection.deleteOne(and(byKey(entry.key), unchanged)).deletedCount > 0
    }

    private fun heldWith(key: String, token: String): Bson = and(byKey(key), repoExpr.token eq token)

    // A lock is inserted with its key as a plain string _id, so it is matched as-is.
    private fun byKey(key: String): Bson = Filters.eq("_id", key)
}
