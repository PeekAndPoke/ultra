package de.peekandpoke.funktor.cluster.workers

import de.peekandpoke.funktor.cluster.workers.api.WorkerModel
import de.peekandpoke.funktor.cluster.workers.services.WorkerHistory
import de.peekandpoke.funktor.cluster.workers.services.WorkerRegistry
import de.peekandpoke.funktor.cluster.workers.services.WorkerTracker
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.log.Log
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class WorkersFacade(
    private val kontainer: Kontainer,
    private val kronos: Kronos,
    private val registry: WorkerRegistry,
    private val tracker: WorkerTracker,
    private val history: WorkerHistory,
    private val log: Log,
) {
    class RunningWorker(
        val worker: Worker,
        val job: Deferred<*>,
    )

    private fun now() = kronos.instantNow()

    /**
     * Runs workers that are due.
     *
     * The [state] is given as a callback as it might change while this method is running.
     * E.g. when the server is shut down we need to stop all workers asap.
     *
     * Setting [forceRun] to true will force all workers to be executed, no matter when the last run has been.
     */
    suspend fun tick(state: StateProvider, forceRun: Boolean = false): List<RunningWorker> {
        // In case we have a mutable kronos, it might be that the clock was fast forwarded and then set back.
        // In this case we need to forget last worker runs that appear to be in the future.
        if (kronos is Kronos.Mutable) {
            tracker.clearFutureRuns(now())
        }

        val context = SupervisorJob() + Dispatchers.IO
        val workers = registry.workers

        val jobs: List<RunningWorker> = coroutineScope {
            workers.mapNotNull { (workerClass, _) ->
                // Each worker gets its own kontainer:
                // -> to avoid side effects between the workers f.e. with the EntityCache
                // -> we reuse the Kronos, in case we have an AdvancedKronos ... this one would load from the database again and again.
                val workerKontainer = kontainer.clone { with { kronos } }

                workerKontainer.getOrNull(workerClass)?.let { worker ->
                    // Should the worker be run
                    val shouldBeRun = state.isRunning && (forceRun || shouldRun(worker))

                    if (shouldBeRun) {
                        tracker.lockWorker(context, worker) {
                            try {
                                executeRun(state = state, worker = worker)
                            } catch (e: Throwable) {
                                log.error("Running worker ${worker.id} failed!\n\n${e.stackTraceToString()}")
                            }
                        }
                    } else {
                        null
                    }
                }
            }
        }

        return jobs
    }

    /**
     * Runs all workers that are due and await all their results.
     *
     * The [state] is given as a callback as it might change while this method is running.
     * E.g. when the server is shut down we need to stop all workers asap.
     */
    suspend fun tickSync(state: StateProvider): List<Any?> {
        return tick(state = state, forceRun = false).map { it.job }.awaitAll()
    }

    /**
     * Runs all workers that are due and await all their results.
     *
     * The [state] is given as a callback as it might change while this method is running.
     * E.g. when the server is shut down we need to stop all workers asap.
     */
    suspend fun tickSyncForced(state: StateProvider): List<Any?> {
        val jobs = tick(state = state, forceRun = true)

        return jobs.map { it.job }.awaitAll()
    }

    suspend fun stats(): List<WorkerModel> = registry.workerIds.map { stats(it) }

    suspend fun stats(id: String): WorkerModel = WorkerModel(
        id = id,
        runs = history.getHistory(id).map {
            WorkerModel.Run(
                serverId = it.serverId,
                begin = MpInstant.fromEpochMillis(it.beginTs),
                end = MpInstant.fromEpochMillis(it.endTs),
                result = it.result,
            )
        }
    )

    private fun shouldRun(worker: Worker): Boolean {
        val lastRun = tracker.getLastRunInstant(worker)

        return worker.shouldRun(lastRun, now())
    }

    private suspend fun executeRun(state: StateProvider, worker: Worker) {

        val begin = now()

        val result = try {
            worker.execute(state)
            WorkerModel.Run.Result.Success
        } catch (t: Throwable) {

            log.error("Error running worker '${worker.id}':\n" + "${t.message}\n" + t.stackTraceToString())

            WorkerModel.Run.Result.Failure.of(t)
        }

        // Update the tracker
        tracker.putLastRunInstant(worker, begin)

        // Record the result
        history.putRun(
            worker = worker,
            begin = begin,
            end = now(),
            result = result,
        )
    }
}
