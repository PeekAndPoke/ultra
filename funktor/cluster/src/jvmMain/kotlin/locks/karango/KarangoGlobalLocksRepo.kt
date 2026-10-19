package io.peekandpoke.funktor.cluster.locks.karango

import com.arangodb.ArangoDBException
import com.arangodb.model.AqlQueryOptions
import io.peekandpoke.funktor.cluster.locks.VaultGlobalLocksProvider
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.created
import io.peekandpoke.funktor.cluster.locks.domain.expires
import io.peekandpoke.funktor.cluster.locks.domain.isLegacy
import io.peekandpoke.funktor.cluster.locks.domain.lastHeartbeat
import io.peekandpoke.funktor.cluster.locks.domain.token
import io.peekandpoke.funktor.core.fixtures.FixtureLoader
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.REMOVE
import io.peekandpoke.karango.aql.RETURN_NEW
import io.peekandpoke.karango.aql.UPDATE
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.buildAqlQuery
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault._key
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.ts
import io.peekandpoke.ultra.reflection.kMapType
import io.peekandpoke.ultra.reflection.kType
import kotlinx.coroutines.future.await

class KarangoGlobalLocksRepo(driver: KarangoDriver, repoName: String) :
    EntityRepository<GlobalLockEntry>(name = repoName, kType(), driver), VaultGlobalLocksProvider.Repo {

    class Fixtures(repo: Lazy<KarangoGlobalLocksRepo>) : FixtureLoader {

        private val repo: KarangoGlobalLocksRepo by repo

        override suspend fun prepare(result: FixtureLoader.MutableResult) {
            result.info("Clearing repo ${repo.name}")
            repo.removeAll()
        }

        override suspend fun load(result: FixtureLoader.MutableResult) {
            // noop
        }
    }

    override suspend fun heartbeat(key: String, token: String, heartbeatAt: MpInstant, expiresAt: MpInstant): Boolean {
        val query = buildAqlQuery {
            FOR(repo) {
                FILTER(it._key EQ key)
                FILTER(it.token EQ token)

                UPDATE(it, repo) {
                    put({ lastHeartbeat }) { heartbeatAt.aql }
                    put({ expires }) { expiresAt.aql }
                }

                RETURN_NEW(it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val vars = driver.codec.slumber(kMapType<String, Any?>().type, query.vars) as Map<String, Any?>

        // Straight to the async driver: KarangoDriver.query runs every query on Dispatchers.IO, which blocking handlers
        // may saturate. The driver does its I/O on its own event loop, so this blocks no thread.
        val cursor = driver.arangoDb.query(query.query, Any::class.java, vars, AqlQueryOptions()).await()

        return cursor.result.isNotEmpty()
    }

    override suspend fun removeIfHeld(key: String, token: String): Boolean {
        val removed = findFirst {
            FOR(repo) {
                FILTER(it._key EQ key)
                FILTER(it.token EQ token)

                REMOVE(it) IN repo
            }
        }

        return removed != null
    }

    override suspend fun removeIfUnchanged(entry: GlobalLockEntry): Boolean {
        val removed = try {
            findFirst {
                FOR(repo) {
                    FILTER(it._key EQ entry.key)

                    when (entry.isLegacy) {
                        // Written without heartbeats: the creation time tells two acquisitions apart.
                        true -> FILTER(it.created.ts EQ entry.created.toEpochMillis())
                        // The same acquisition, and no heartbeat since it was read.
                        false -> {
                            FILTER(it.token EQ entry.token)
                            FILTER(it.lastHeartbeat.ts EQ entry.lastHeartbeat.toEpochMillis())
                        }
                    }

                    REMOVE(it) IN repo
                }
            }
        } catch (e: Throwable) {
            // Changed or removed by someone else at the same moment, e.g. by a contender taking it over as well.
            if (e.isWriteWriteConflict()) return false else throw e
        }

        return removed != null
    }

    private fun Throwable.isWriteWriteConflict(): Boolean = generateSequence(this) { it.cause }
        .filterIsInstance<ArangoDBException>()
        .any { it.errorNum == WRITE_WRITE_CONFLICT }

    private companion object {
        const val WRITE_WRITE_CONFLICT = 1200
    }
}
