package de.peekandpoke.ktorfx.cluster.backgroundjobs.workers

import de.peekandpoke.ktorfx.cluster.backgroundjobs.BackgroundJobs
import de.peekandpoke.ktorfx.cluster.workers.StateProvider
import de.peekandpoke.ktorfx.cluster.workers.Worker
import de.peekandpoke.ultra.common.datetime.MpInstant

class BackgroundJobsWorker(
    private val backgroundJobs: BackgroundJobs,
) : Worker {

    override val shouldRun: (lastRun: MpInstant, now: MpInstant) -> Boolean = Worker.Every.milliseconds(500)

    override suspend fun execute(state: StateProvider) {
        backgroundJobs.runQueuedJobs(state)
    }
}
