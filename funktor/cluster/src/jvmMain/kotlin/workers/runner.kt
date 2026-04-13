package io.peekandpoke.funktor.cluster.workers

import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleBuilder
import io.peekandpoke.ultra.kontainer.Kontainer

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

/** Callback returning the current [WorkersRunner.State]; checked by workers to detect shutdown. */
typealias StateProvider = () -> WorkersRunner.State

/** Convenience: true when the state is [WorkersRunner.State.Running]. */
val StateProvider.isRunning get() = this().isRunning
