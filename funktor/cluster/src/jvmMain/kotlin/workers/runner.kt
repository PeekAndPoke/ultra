package de.peekandpoke.funktor.cluster.workers

import de.peekandpoke.funktor.core.lifecycle.AppLifeCycleBuilder
import de.peekandpoke.ultra.kontainer.Kontainer

/**
 * Call this function to launch the background workers
 */
fun AppLifeCycleBuilder.launchWorkers(
    initialDelayMs: Long = 3_000,
    intervalMs: Long = 100,
    kontainerProvider: () -> Kontainer,
): WorkersRunner = WorkersRunner(
    lifeCycleBuilder = this,
    initialDelayMs = initialDelayMs,
    intervalMs = intervalMs,
    kontainerProvider = kontainerProvider
)

typealias StateProvider = () -> WorkersRunner.State

val StateProvider.isRunning get() = this().isRunning

