package de.peekandpoke.funktor.cluster.workers.example

import de.peekandpoke.funktor.cluster.workers.StateProvider
import de.peekandpoke.funktor.cluster.workers.Worker
import de.peekandpoke.funktor.cluster.workers.isRunning
import de.peekandpoke.ultra.log.Log
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ExampleWorker(
    private val delay: Duration,
    private val log: Log,
) : Worker {

    override val shouldRun = Worker.Every(5.minutes)

    override suspend fun execute(state: StateProvider) {
        if (state.isRunning) {
            log.debug("ExampleWorker is running and waiting for ${delay.toIsoString()}")

            delay(delay.inWholeMilliseconds)

            log.debug("ExampleWorker is done")
        }
    }
}
