package io.peekandpoke.funktor.cluster.workers.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class WorkerTrackerSpec : StringSpec({

    beforeTest {
        WorkerTracker.clear()
    }

    afterSpec {
        WorkerTracker.clear()
    }

    // lockWorker //////////////////////////////////////////////////////////////////////////////////

    "lockWorker returns a RunningWorker on the first lock for a given worker id" {
        val worker = TestWorker(id = "w1")
        val started = CompletableDeferred<Unit>()
        val context: CoroutineContext = SupervisorJob() + Dispatchers.IO

        val running = WorkerTracker.lockWorker(context, worker) {
            started.complete(Unit)
            delay(50)
        }

        running.shouldNotBeNull()
        running.worker.id shouldBe "w1"

        withTimeout(5_000) { started.await() }
        running.job.await()
    }

    "lockWorker returns null when the same worker id is already locked" {
        val worker = TestWorker(id = "w-busy")
        val firstStarted = CompletableDeferred<Unit>()
        val firstCanFinish = CompletableDeferred<Unit>()
        val context: CoroutineContext = SupervisorJob() + Dispatchers.IO

        val first = WorkerTracker.lockWorker(context, worker) {
            firstStarted.complete(Unit)
            firstCanFinish.await()
        }
        first.shouldNotBeNull()

        withTimeout(5_000) { firstStarted.await() }

        val second = WorkerTracker.lockWorker(context, worker) { /* never runs */ }
        second shouldBe null

        firstCanFinish.complete(Unit)
        first.job.await()
    }

    // Regression for HIGH #1 //////////////////////////////////////////////////////////////////////

    "clear() cancels a long-running worker block (regression for HIGH #1)" {
        val worker = TestWorker(id = "w-cancellable")
        val completedNormally = AtomicBoolean(false)
        val started = CompletableDeferred<Unit>()
        val context: CoroutineContext = SupervisorJob() + Dispatchers.IO

        val running = WorkerTracker.lockWorker(context, worker) {
            started.complete(Unit)
            // Long delay that MUST be interrupted by clear() for this test to pass.
            delay(60_000)
            completedNormally.set(true)
        }
        running.shouldNotBeNull()

        withTimeout(5_000) { started.await() }

        WorkerTracker.clear()

        // Give the cancellation a moment to propagate.
        withTimeout(5_000) {
            while (!running.job.isCancelled && !running.job.isCompleted) {
                delay(10)
            }
        }

        running.job.isCancelled shouldBe true
        completedNormally.get() shouldBe false
    }

    // lastRuns ////////////////////////////////////////////////////////////////////////////////////

    "putLastRunInstant / getLastRunInstant round-trip the stored value" {
        val worker = TestWorker(id = "w-stamp")
        val stamp = MpInstant.fromEpochSeconds(1_700_000_000)

        WorkerTracker.putLastRunInstant(worker, stamp)

        WorkerTracker.getLastRunInstant(worker) shouldBe stamp
    }

    "getLastRunInstant returns MpInstant.Epoch for an unknown worker" {
        val worker = TestWorker(id = "w-unknown")

        WorkerTracker.getLastRunInstant(worker) shouldBe MpInstant.Epoch
    }

    // Regression for HIGH #2 //////////////////////////////////////////////////////////////////////

    "concurrent putLastRunInstant from many coroutines preserves all writes (regression for HIGH #2)" {
        val count = 1000
        val stamp = MpInstant.fromEpochSeconds(1_700_000_000)
        val workers = (0 until count).map { TestWorker(id = "w-$it") }

        coroutineScope {
            workers.map { w ->
                async(Dispatchers.Default) {
                    WorkerTracker.putLastRunInstant(w, stamp)
                }
            }.awaitAll()
        }

        // Every write must be readable — no lost updates, no exceptions.
        workers.forEach { w ->
            WorkerTracker.getLastRunInstant(w) shouldBe stamp
        }
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

private class TestWorker(override val id: String) : Worker {
    override val shouldRun: (MpInstant, MpInstant) -> Boolean = { _, _ -> true }
    override suspend fun execute(state: StateProvider) { /* unused by WorkerTracker tests */
    }
}
