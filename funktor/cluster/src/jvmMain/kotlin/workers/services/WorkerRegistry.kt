package de.peekandpoke.ktorfx.cluster.workers.services

import de.peekandpoke.ktorfx.cluster.workers.Worker
import de.peekandpoke.ultra.common.Lookup

/**
 * Registry that injects all [Worker] instances.
 */
class WorkerRegistry(
    workersLookup: Lookup<Worker>,
) {
    val workers = workersLookup.all().map { it::class to it.id }

    @Suppress("unused")
    val workerClasses = workers.map { it.first }

    val workerIds = workers.map { it.second }
}
