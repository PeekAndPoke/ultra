package de.peekandpoke.ktorfx.cluster.workers.vault

import de.peekandpoke.karango._key
import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.GT
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.OR
import de.peekandpoke.karango.aql.REMOVE
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.workers.domain.endTs
import de.peekandpoke.ktorfx.cluster.workers.domain.workerId
import de.peekandpoke.ktorfx.cluster.workers.services.WorkerHistory
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Stored

class KarangoWorkerHistoryRepo(
    driver: KarangoDriver,
    repoName: String,
) : WorkerHistory.Adapter.Vault.Repo, EntityRepository<VaultWorkerRun>(
    name = repoName,
    storedType = kType(),
    driver = driver,
) {
    override fun IndexBuilder<VaultWorkerRun>.buildIndexes() {
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
