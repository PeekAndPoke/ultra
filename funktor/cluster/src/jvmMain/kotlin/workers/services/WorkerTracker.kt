package de.peekandpoke.funktor.cluster.workers.services

import de.peekandpoke.funktor.cluster.workers.Worker
import de.peekandpoke.funktor.cluster.workers.WorkersFacade
import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Keeps track of workers.
 *
 * Prevents that the same worker is being run multiple times in parallel.
 */
object WorkerTracker {

    private class JobRef(
        var job: Job?,
    )

    private val lock = Any()

    private val lastRuns = mutableMapOf<String, MpInstant>()

    private val runningWorkers = mutableMapOf<String, JobRef>()

    fun clear() {
        sync {
            lastRuns.clear()

            runningWorkers.forEach {
                try {
                    it.value.job?.cancel()
                } catch (_: Throwable) {
                }
            }

            runningWorkers.clear()
        }
    }

    fun clearFutureRuns(instant: MpInstant) {
        // We do this to clear last runs that are in the future
        // This is only useful for development, when the system clock is fast forwarded

        sync {
            val toBeRemoved = lastRuns.toList().filter { (_, lastRun) -> lastRun > instant }.map { (k, _) -> k }.toSet()
            val toBeCancelled = runningWorkers.toMap().filter { (k, _) -> k in toBeRemoved }.map { it.value }

            toBeRemoved.forEach {
                // Clear last runs
                lastRuns.remove(it)
                // Remove from running workers
                runningWorkers.remove(it)
            }

            toBeCancelled.forEach { it.job?.cancel() }
        }
    }

    suspend fun lockWorker(
        context: CoroutineContext,
        worker: Worker,
        block: suspend () -> Unit,
    ): WorkersFacade.RunningWorker? {

        val workerId = worker.id

        val locked = sync {
            // If the worker is already running we leave
            if (runningWorkers.contains(workerId)) {
                // False means locking failed
                false
            } else {
                // Otherwise, we mark the worker as running
                runningWorkers[workerId] = JobRef(null)
                // True means locking succeeded
                true
            }
        }

        if (!locked) {
            return null
        }

        val released = AtomicBoolean(false)

        val release: () -> Unit = {
            // Make sure that we do not release the worker twice
            if (!released.get()) {
                released.set(true)
                releaseWorker(worker)
            }
        }

        val job = coroutineScope {
            async(context) {
                block()
            }
        }

        runningWorkers[workerId]?.job = job

        job.invokeOnCompletion { release() }

        return WorkersFacade.RunningWorker(worker, job)
    }

    private fun releaseWorker(worker: Worker) {
        val workerId = worker.id

        sync {
            runningWorkers.remove(workerId)
        }
    }

    internal fun putLastRunInstant(worker: Worker, instant: MpInstant) {
        lastRuns[worker.id] = instant
    }

    internal fun getLastRunInstant(worker: Worker): MpInstant {
        return getLastRunInstant(workerId = worker.id)
    }

    private fun getLastRunInstant(workerId: String): MpInstant {
        return lastRuns[workerId] ?: MpInstant.Epoch
    }

    private fun <T> sync(block: () -> T): T {
        return synchronized(lock, block)
    }
}
