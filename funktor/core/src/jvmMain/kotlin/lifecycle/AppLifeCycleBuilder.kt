package io.peekandpoke.funktor.core.lifecycle

import io.ktor.events.EventDefinition
import io.ktor.events.EventHandler
import io.ktor.server.application.*
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks.ExecutionOrder
import io.peekandpoke.ultra.kontainer.Kontainer
import kotlinx.coroutines.runBlocking

class AppLifeCycleBuilder internal constructor(private val app: Application) {

    internal val listeners = mutableListOf<AppLifeCycle.EventAndHandler<out Any?>>()

    /**
     * Registers a generic [handler] for an [event] .
     */
    fun <T : Any> on(event: EventDefinition<T>, handler: EventHandler<T>) {
        listeners.add(
            AppLifeCycle.EventAndHandler(
                event = event,
                handler = handler,
            )
        )
    }

    /**
     * Registers all hooks by getting the [AppLifeCycleHooks] service from the given [kontainer].
     */
    suspend fun register(kontainer: Kontainer) {
        kontainer.getOrNull(AppLifeCycleHooks::class)?.let { register(it) }
    }

    /**
     * Registers all [hooks] given.
     */
    suspend fun register(hooks: AppLifeCycleHooks) {
        // onAppStarting fires inline during lifeCycle() — suspend hooks run directly.
        // AppStartException is a fatal signal and must abort startup.
        runHooks(hooks.onAppStarting, "OnAppStarting", { it.executionOrder }, rethrow = { it is AppStartException }) {
            it.onAppStarting(app)
        }

        // onAppStarted / StopPreparing / Stopping / Stopped fire via Ktor's non-suspend
        // EventHandler. We bridge to suspend via runBlocking rooted in the Application's
        // coroutineContext so we never create a detached scope.
        onAppStarted { application ->
            runBlocking(app.coroutineContext) {
                runHooks(hooks.onAppStarted, "OnAppStarted", { it.executionOrder }) {
                    it.onAppStarted(application)
                }
            }
        }

        onAppStopPreparing { environment ->
            runBlocking(app.coroutineContext) {
                runHooks(hooks.onAppStopPreparing, "OnAppStopPreparing", { it.executionOrder }) {
                    it.onAppStopPreparing(environment)
                }
            }
        }

        onAppStopping { application ->
            runBlocking(app.coroutineContext) {
                runHooks(hooks.onAppStopping, "OnAppStopping", { it.executionOrder }) {
                    it.onAppStopping(application)
                }
            }
        }

        onAppStopped { application ->
            runBlocking(app.coroutineContext) {
                runHooks(hooks.onAppStopped, "OnAppStopped", { it.executionOrder }) {
                    it.onAppStopped(application)
                }
            }
        }
    }

    private suspend fun <T : Any> runHooks(
        hooks: List<T>,
        phase: String,
        order: (T) -> ExecutionOrder,
        rethrow: (Throwable) -> Boolean = { false },
        invoke: suspend (T) -> Unit,
    ) {
        hooks.sortedByDescending(order).forEach { hook ->
            val name = hook::class.qualifiedName

            app.log.info("Running $phase hook $name with priority ${order(hook).priority}")

            try {
                invoke(hook)
            } catch (e: Throwable) {
                if (rethrow(e)) throw e
                app.log.error("$phase hook $name failed!", e)
            }
        }
    }

    /**
     * Registers a hook that is run immediately, before the app is started.
     */
    suspend fun onAppStarting(handler: suspend (Application) -> Unit) {
        handler(app)
    }

    /**
     * Registers a hook that is run when the application is started.
     *
     * See [ApplicationStarted]
     */
    fun onAppStarted(handler: EventHandler<Application>) = on(ApplicationStarted, handler)

    /**
     * Registers a hook that is run when application is preparing to stop.
     *
     * See [ApplicationStopPreparing]
     */
    fun onAppStopPreparing(handler: EventHandler<ApplicationEnvironment>) = on(ApplicationStopPreparing, handler)

    /**
     * Registers a hook that is run when the application is stopping.
     *
     * See [ApplicationStopping]
     */
    fun onAppStopping(handler: EventHandler<Application>) = on(ApplicationStopping, handler)

    /**
     * Registers a hook that is run when the application has stopped.
     *
     * See [ApplicationStopped]
     */
    fun onAppStopped(handler: EventHandler<Application>) = on(ApplicationStopped, handler)
}
