package io.peekandpoke.funktor.core.lifecycle

import io.ktor.server.application.*
import io.peekandpoke.ultra.kontainer.Kontainer

private val appRegistry = mutableMapOf<Application, AppLifeCycle>()

fun Application.lifeCycle(kontainer: Kontainer, builder: AppLifeCycleBuilder.() -> Unit) {

    val built = AppLifeCycleBuilder(app = this).apply {
        onAppStarting { log.info("----====  LifeCycle onAppStarting  ====----") }
        onAppStarted { log.info("----====  LifeCycle onAppStarted  ====----") }
        onAppStopPreparing { log.info("----====  LifeCycle onAppStopPreparing  ====----") }
        onAppStopping { log.info("----====  LifeCycle onAppStopping  ====----") }
        onAppStopped { log.info("----====  LifeCycle onAppStopped  ====----") }

        register(kontainer)

        builder()
    }

    val lifeCycle = appRegistry.getOrPut(this) { AppLifeCycle(this) }

    lifeCycle.register(built.listeners)
}
