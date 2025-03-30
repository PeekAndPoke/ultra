package de.peekandpoke.ktorfx.cluster.workers.fixtures

import de.peekandpoke.ktorfx.cluster.workers.services.WorkerHistory
import de.peekandpoke.ktorfx.cluster.workers.services.WorkerTracker
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader

class WorkerFixtures(
    private val history: WorkerHistory,
    private val tracker: WorkerTracker,
) : FixtureLoader {

    override suspend fun prepare(result: FixtureLoader.MutableResult) {
    }

    override suspend fun load(result: FixtureLoader.MutableResult) {
    }

    override suspend fun finalize(result: FixtureLoader.MutableResult) {
        result.info("Clearing worker history")
        // Clear the worker results history
        history.clear()

        result.info("Resetting worker tracker")
        // Clear the worker registry
        tracker.clear()
    }
}
