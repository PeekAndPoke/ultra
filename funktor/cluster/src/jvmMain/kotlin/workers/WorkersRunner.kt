package io.peekandpoke.funktor.cluster.workers

import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleBuilder
import io.peekandpoke.ultra.kontainer.Kontainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Lifecycle-aware runner that periodically ticks the [WorkersFacade] on a background coroutine. */
class WorkersRunner(
    lifeCycleBuilder: AppLifeCycleBuilder,
    initialDelayMs: Long,
    intervalMs: Long,
    kontainerProvider: () -> Kontainer,
) {
    enum class State {
        Running,
        Stopped;

        val isRunning get() = this == Running
    }

    private var state = State.Running

    init {
        with(lifeCycleBuilder) {

            onAppStarted { app ->
                val scope = SupervisorJob() + Dispatchers.IO

                app.launch(scope) {
                    delay(initialDelayMs)

                    while (state.isRunning) {
                        try {
                            val kontainer: Kontainer = kontainerProvider()
                            kontainer.getOrNull(WorkersFacade::class)?.tick(state = { state })
                        } catch (e: Throwable) {
                            app.log.error("Running the Workers failed", e)
                        }

                        delay(intervalMs)
                    }
                }
            }

            onAppStopping { app ->
                app.log.info("Stopping workers")
                stop()
            }
        }
    }

    fun stop() {
        state = State.Stopped
    }
}
