package io.peekandpoke.funktor.cluster.workers.vault

import io.peekandpoke.funktor.cluster.workers.domain.endTs
import io.peekandpoke.funktor.cluster.workers.domain.workerId
import io.peekandpoke.funktor.cluster.workers.services.WorkerHistory
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.GT
import io.peekandpoke.karango.aql.IS_NULL
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.OR
import io.peekandpoke.karango.aql.REMOVE
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.karango.vault._key
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Stored

class KarangoWorkerHistoryRepo(
    driver: KarangoDriver,
    repoName: String,
) : WorkerHistory.Adapter.Vault.Repo, EntityRepository<VaultWorkerRun>(
    name = repoName,
    storedType = kType(),
    driver = driver,
) {
    override fun KarangoIndexBuilder<VaultWorkerRun>.buildIndexes() {
        persistentIndex {
            field { run.endTs }
            field { run.workerId }
            field { status }
        }
    }

    override suspend fun getHistoryByWorker(workerId: String, limit: Int): Cursor<Stored<VaultWorkerRun>> = find {
        FOR(repo) { entry ->
            FILTER(entry.run.workerId EQ workerId)
            SORT(entry.run.endTs.DESC)

            LIMIT(limit)

            RETURN(entry)
        }
    }

    override suspend fun removeAllButLastSuccessful(workerId: String, limit: Int): Int {
        val result = find {
            val toDelete = LET(
                "toDelete",
                FOR(repo) { entry ->
                    FILTER(entry.run.workerId EQ workerId)
                    FILTER(
                        (entry.status EQ VaultWorkerRun.Status.Success) OR IS_NULL(entry.status)
                    )
                    SORT(entry.run.endTs.DESC)

                    // Skip 'limit'
                    SKIP(limit)

                    RETURN(entry)
                }
            )

            FOR(toDelete) { entry ->
                REMOVE(entry._key).IN(repo) {
                    ignoreErrors = true
                }
            }
        }

        return result.count()
    }

    override suspend fun removeAllEndedAfter(after: MpInstant): Int {
        val result = find {
            FOR(repo) { entry ->
                FILTER(entry.run.endTs GT after.toEpochMillis())
                REMOVE(entry) IN repo
            }
        }

        return result.count()
    }
}
