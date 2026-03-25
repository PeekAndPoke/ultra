package io.peekandpoke.funktor.cluster.backgroundjobs.workers

import io.peekandpoke.funktor.cluster.backgroundjobs.BackgroundJobs
import io.peekandpoke.funktor.cluster.workers.StateProvider
import io.peekandpoke.funktor.cluster.workers.Worker
import io.peekandpoke.ultra.datetime.MpInstant

class BackgroundJobsWorker(
    private val backgroundJobs: BackgroundJobs,
) : Worker {

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.milliseconds(500)

    override suspend fun execute(state: StateProvider) {
        backgroundJobs.runQueuedJobs(state)
    }
}
