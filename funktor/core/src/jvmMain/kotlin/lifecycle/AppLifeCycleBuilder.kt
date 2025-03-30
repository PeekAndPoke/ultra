package de.peekandpoke.ktorfx.core.lifecycle

import de.peekandpoke.ultra.kontainer.Kontainer
import io.ktor.events.EventDefinition
import io.ktor.events.EventHandler
import io.ktor.server.application.*
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
    fun register(kontainer: Kontainer) {
        kontainer.getOrNull(AppLifeCycleHooks::class)?.let { register(it) }
    }

    /**
     * Registers all [hooks] given.
     */
    fun register(hooks: AppLifeCycleHooks) {
        onAppStarting {
            runBlocking {
                hooks.onAppStarting.sortedByDescending { it.executionOrder }.forEach { hook ->

                    val name = hook::class.qualifiedName

                    app.log.info("Running OnAppStarting hook $name with priority ${hook.executionOrder.priority}")

                    try {
                        hook.onAppStarting(app)
                    } catch (e: Throwable) {
                        app.log.error("OnAppStarting hook $name failed!", e)
                    }
                }
            }
        }

        onAppStarted { application ->
            runBlocking {
                hooks.onAppStarted.sortedByDescending { it.executionOrder }.forEach { hook ->
                    val name = hook::class.qualifiedName

                    app.log.info("Running OnAppStarted hook $name with priority ${hook.executionOrder.priority}")

                    try {
                        hook.onAppStarted(application)
                    } catch (e: Throwable) {
                        app.log.error("OnAppStarted hook $name failed!", e)
                    }
                }
            }
        }

        onAppStopPreparing { application ->
            runBlocking {
                hooks.onAppStopPreparing.sortedByDescending { it.executionOrder }.forEach { hook ->
                    val name = hook::class.qualifiedName

                    app.log.info("Running OnAppStopPreparing hook $name with priority ${hook.executionOrder.priority}")

                    try {
                        hook.onAppStopPreparing(application)
                    } catch (e: Throwable) {
                        app.log.error("OnAppStopPreparing hook $name failed!", e)
                    }
                }
            }
        }

        onAppStopping { application ->
            runBlocking {
                hooks.onAppStopping.sortedByDescending { it.executionOrder }.forEach { hook ->
                    val name = hook::class.qualifiedName

                    app.log.info("Running OnAppStopping hook $name with priority ${hook.executionOrder.priority}")

                    try {
                        hook.onAppStopping(application)
                    } catch (e: Throwable) {
                        app.log.error("OnAppStopping hook $name failed!", e)
                    }
                }
            }
        }

        onAppStopped { application ->
            runBlocking {
                hooks.onAppStopped.sortedByDescending { it.executionOrder }.forEach { hook ->
                    val name = hook::class.qualifiedName

                    app.log.info("Running OnAppStopped hook $name with priority ${hook.executionOrder.priority}")

                    try {
                        hook.onAppStopped(application)
                    } catch (e: Throwable) {
                        app.log.error("OnAppStopped hook $name failed!", e)
                    }
                }
            }
        }
    }

    /**
     * Registers a hook that is run immediately, before the app is started.
     */
    fun onAppStarting(handler: EventHandler<Application>) {
        runBlocking {
            handler(app)
        }
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
