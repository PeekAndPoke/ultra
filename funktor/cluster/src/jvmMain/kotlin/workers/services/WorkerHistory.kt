package de.peekandpoke.ktorfx.cluster.workers.services

import de.peekandpoke.ktorfx.cluster.locks.GlobalServerId
import de.peekandpoke.ktorfx.cluster.workers.Worker
import de.peekandpoke.ktorfx.cluster.workers.api.WorkerModel
import de.peekandpoke.ktorfx.cluster.workers.domain.WorkerRun
import de.peekandpoke.ktorfx.cluster.workers.vault.VaultWorkerRun
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.BatchInsertRepository
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WorkerHistory(
    private val adapter: Adapter,
    private val serverId: GlobalServerId,
) {
    interface Adapter {

        suspend fun clear()

        suspend fun getHistory(workerId: String, limit: Int): List<WorkerRun>

        suspend fun getHistory(worker: Worker, limit: Int): List<WorkerRun> {
            return getHistory(workerId = worker.id, limit = limit)
        }

        suspend fun putRun(run: WorkerRun)

        object InMemory : Adapter {
            private var lastRuns = mutableMapOf<String, MutableList<WorkerRun>>()

            override suspend fun clear() {
                lastRuns.clear()
            }

            override suspend fun getHistory(workerId: String, limit: Int): List<WorkerRun> {
                return getList(workerId).take(limit)
            }

            override suspend fun putRun(run: WorkerRun) {
                val list = getList(run.workerId)

                list.add(0, run)

                while (list.size > 1000) {
                    list.removeLast()
                }
            }

            private fun getList(id: String): MutableList<WorkerRun> {
                return lastRuns.getOrPut(id) { mutableListOf() }
            }
        }

        class Vault(private val inner: Repo) : Adapter {

            companion object {
                private val kronos = Kronos.systemUtc
                private val buffer: MutableList<VaultWorkerRun> = mutableListOf()
                private var lastFlush: MpInstant = kronos.instantNow()
                private var lastCleanup: MpInstant = kronos.instantNow()
            }

            interface Repo : Repository<VaultWorkerRun>, BatchInsertRepository<VaultWorkerRun> {
                suspend fun getHistoryByWorker(workerId: String, limit: Int): Cursor<Stored<VaultWorkerRun>>

                /**
                 * Deletes all success result and keeps the newest [limit].
                 * Returns the number of deleted entries
                 */
                suspend fun removeAllButLastSuccessful(workerId: String, limit: Int): Int

                /**
                 * Deletes all entries that are created after [after].
                 */
                suspend fun removeAllEndedAfter(after: MpInstant): Int
            }

            override suspend fun clear() {
                inner.removeAll()
            }

            override suspend fun getHistory(workerId: String, limit: Int): List<WorkerRun> {
                return inner.getHistoryByWorker(workerId = workerId, limit = limit)
                    .map { it.value.run }
            }

            override suspend fun putRun(run: WorkerRun) {
                coroutineScope {
                    launch(Dispatchers.IO) {
                        synchronized(buffer) {
                            buffer.add(VaultWorkerRun(run = run))
                        }

                        flush()
                        cleanup(run)
                    }
                }
            }

            private suspend fun flush() {
                val now = now()

                val elapsed = (now - lastFlush)

                if (elapsed > 10.seconds) {
                    val copied = synchronized(buffer) {
                        lastFlush = now
                        buffer.toList().also {
                            buffer.clear()
                        }
                    }

                    // Batch insert the latest
                    inner.batchInsertValues(copied)

                    // Cleanup old results
                    val workerIds = copied.map { it.run.workerId }.toSet()

                    // Clean up for the given worker id
                    workerIds.forEach { workerId ->
                        try {
                            inner.removeAllButLastSuccessful(workerId = workerId, limit = 1000)
                        } finally {
                        }
                    }
                }
            }

            private suspend fun cleanup(lastRun: WorkerRun) {
                val now = now()

                val elapsed = (now - lastCleanup)

                if (elapsed > 60.seconds) {
                    lastCleanup = now

                    // NOTICE: this is a work-around ... on dev systems the clock might be reset and the db might contain future entries
                    inner.removeAllEndedAfter(
                        MpInstant.fromEpochMillis(lastRun.endTs).plus(5.minutes)
                    )
                }
            }

            private fun now() = kronos.instantNow()
        }
    }

    suspend fun clear() = adapter.clear()

    suspend fun getHistory(worker: Worker, limit: Int = 100): List<WorkerRun> = getHistory(worker.id, limit)

    suspend fun getHistory(id: String, limit: Int = 100): List<WorkerRun> = adapter.getHistory(id, limit)

    suspend fun putRun(
        worker: Worker,
        begin: MpInstant,
        end: MpInstant,
        result: WorkerModel.Run.Result,
    ) {
        val run = WorkerRun(
            serverId = serverId.getId(),
            workerId = worker.id,
            beginTs = begin.toEpochMillis(),
            endTs = end.toEpochMillis(),
            result = result,
        )

        adapter.putRun(run)
    }
}
