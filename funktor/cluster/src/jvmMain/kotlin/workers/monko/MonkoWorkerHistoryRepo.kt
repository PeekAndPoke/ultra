package io.peekandpoke.funktor.cluster.workers.monko

import com.mongodb.client.model.Filters
import io.peekandpoke.funktor.cluster.workers.domain.endTs
import io.peekandpoke.funktor.cluster.workers.domain.workerId
import io.peekandpoke.funktor.cluster.workers.services.WorkerHistory
import io.peekandpoke.funktor.cluster.workers.vault.VaultWorkerRun
import io.peekandpoke.funktor.cluster.workers.vault.run
import io.peekandpoke.funktor.cluster.workers.vault.status
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoIndexBuilder
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.or
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.BatchInsertRepository
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.map

class MonkoWorkerHistoryRepo(
    driver: MonkoDriver,
    repoName: String,
) : WorkerHistory.Adapter.Vault.Repo, MonkoRepository<VaultWorkerRun>(
    name = repoName,
    storedType = kType(),
    driver = driver,
), BatchInsertRepository<VaultWorkerRun> {

    override fun MonkoIndexBuilder<VaultWorkerRun>.buildIndexes() {
        persistentIndex {
            field { it.`run`.endTs }
            field { it.`run`.workerId }
            field { it.status }
        }
    }

    override suspend fun getHistoryByWorker(workerId: String, limit: Int): Cursor<Stored<VaultWorkerRun>> = find { r ->
        filter(r.`run`.workerId eq workerId)
        sort(r.`run`.endTs.desc)
        limit(limit)
    }

    override suspend fun removeAllButLastSuccessful(workerId: String, limit: Int): Int {
        val statusFieldPath = repoExpr.status.toFieldPath()
        val workerIdFieldPath = repoExpr.`run`.workerId.toFieldPath()
        val endTsFieldPath = repoExpr.`run`.endTs.toFieldPath()

        // Find IDs to keep: the newest 'limit' successful entries for this worker
        val toKeep = find { r ->
            filter(
                and(
                    r.`run`.workerId eq workerId,
                    or(
                        Filters.eq(statusFieldPath, VaultWorkerRun.Status.Success.name),
                        Filters.eq(statusFieldPath, null),
                    ),
                )
            )
            sort(r.`run`.endTs.desc)
            limit(limit)
        }

        val keepIds = toKeep.map { it._key }

        // Delete all successful entries for this worker except the ones we're keeping
        val deleteFilter = and(
            Filters.eq(workerIdFieldPath, workerId),
            or(
                Filters.eq(statusFieldPath, VaultWorkerRun.Status.Success.name),
                Filters.eq(statusFieldPath, null),
            ),
            Filters.nin("_id", keepIds),
        )

        val result = driver.deleteMany(collection = name, filter = deleteFilter)
        return result.count.toInt()
    }

    override suspend fun removeAllEndedAfter(after: MpInstant): Int {
        val endTsFieldPath = repoExpr.`run`.endTs.toFieldPath()

        val result = driver.deleteMany(
            collection = name,
            filter = Filters.gt(endTsFieldPath, after.toEpochMillis()),
        )
        return result.count.toInt()
    }

    override suspend fun <X : VaultWorkerRun> batchInsert(values: List<New<X>>): List<Stored<X>> {
        return values.map { new ->
            insert(new)
        }
    }
}
