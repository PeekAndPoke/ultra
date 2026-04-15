package io.peekandpoke.funktor.core.lifecycle

import io.ktor.server.application.*
import io.peekandpoke.ultra.kontainer.Kontainer
import kotlinx.coroutines.runBlocking

private val appRegistry = mutableMapOf<Application, AppLifeCycle>()

fun Application.lifeCycle(kontainer: Kontainer, builder: suspend AppLifeCycleBuilder.() -> Unit) {

    runBlocking(coroutineContext) {
        val built = AppLifeCycleBuilder(app = this@lifeCycle).apply {
            onAppStarting { log.info("----====  LifeCycle onAppStarting  ====----") }
            onAppStarted { log.info("----====  LifeCycle onAppStarted  ====----") }
            onAppStopPreparing { log.info("----====  LifeCycle onAppStopPreparing  ====----") }
            onAppStopping { log.info("----====  LifeCycle onAppStopping  ====----") }
            onAppStopped { log.info("----====  LifeCycle onAppStopped  ====----") }

            register(kontainer)

            builder()
        }

        val lifeCycle = appRegistry.getOrPut(this@lifeCycle) { AppLifeCycle(this@lifeCycle) }

        lifeCycle.register(built.listeners)
    }
}
