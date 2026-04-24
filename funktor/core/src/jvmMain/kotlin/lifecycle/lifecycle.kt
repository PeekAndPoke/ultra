package io.peekandpoke.funktor.core.lifecycle

import io.ktor.server.application.*
import io.peekandpoke.ultra.kontainer.Kontainer
import kotlinx.coroutines.runBlocking

private val appRegistry = mutableMapOf<Application, AppLifeCycle>()

/**
 * Wires kontainer-driven lifecycle hooks into the [Application]. Auto-installed once by
 * [io.peekandpoke.funktor.core.App.Definition.module]; do not call from user code.
 *
 * Fires all [AppLifeCycleHooks.OnAppStarting] inline (Ktor's `ApplicationStarting` event has
 * already passed by the time module functions run) and subscribes handlers for the remaining
 * four phases to the app monitor. Banner log lines are emitted once here.
 */
internal fun Application.setupLifecycle(kontainer: Kontainer) {
    runBlocking(coroutineContext) {
        val built = AppLifeCycleBuilder(app = this@setupLifecycle).apply {
            onAppStarting { log.info("----====  LifeCycle onAppStarting  ====----") }
            onAppStarted { log.info("----====  LifeCycle onAppStarted  ====----") }
            onAppStopPreparing { log.info("----====  LifeCycle onAppStopPreparing  ====----") }
            onAppStopping { log.info("----====  LifeCycle onAppStopping  ====----") }
            onAppStopped { log.info("----====  LifeCycle onAppStopped  ====----") }

            register(kontainer)
        }

        val lifeCycle = appRegistry.getOrPut(this@setupLifecycle) { AppLifeCycle(this@setupLifecycle) }

        lifeCycle.register(built.listeners)
    }
}

/**
 * Appends additional lifecycle listeners. Can be called zero, one, or many times from user
 * code — each call adds to the set of listeners installed by [setupLifecycle].
 */
fun Application.lifeCycle(builder: suspend AppLifeCycleBuilder.() -> Unit) {
    runBlocking(coroutineContext) {
        val built = AppLifeCycleBuilder(app = this@lifeCycle).apply { builder() }

        val lifeCycle = appRegistry.getOrPut(this@lifeCycle) { AppLifeCycle(this@lifeCycle) }

        lifeCycle.register(built.listeners)
    }
}
