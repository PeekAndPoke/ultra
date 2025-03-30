package de.peekandpoke.ktorfx.core.lifecycle

import io.ktor.events.EventDefinition
import io.ktor.events.EventHandler
import io.ktor.server.application.*

internal class AppLifeCycle(val app: Application) {

    class EventAndHandler<T>(
        val event: EventDefinition<T>,
        val handler: EventHandler<T>,
    )

    private var registeredListeners = listOf<EventAndHandler<out Any?>>()

    fun register(listeners: List<EventAndHandler<out Any?>>) {

        // We remove all previous registered listeners.
        // This is only necessary for development.
        unsubscribeAll()

        val withInternal = listeners
            // Adding the internal cleanup listener
            .plus(
                EventAndHandler(ApplicationStopped) {
                    it.log.info("LifeCycle: cleaning up")
                    unsubscribeAll()
                }
            )

        withInternal.forEach {
            @Suppress("UNCHECKED_CAST")
            app.monitor.subscribe(it.event, it.handler as EventHandler<Any?>)
        }

        // Remember the listeners
        registeredListeners = withInternal

        app.log.info("LifeCycle: added ${registeredListeners.size} listeners")
    }

    private fun unsubscribeAll() {

        app.log.info("LifeCycle: unsubscribing ${registeredListeners.size} listeners")

        registeredListeners.forEach {
            @Suppress("UNCHECKED_CAST")
            app.monitor.unsubscribe(it.event, it.handler as EventHandler<Any?>)
        }

        registeredListeners = emptyList()
    }
}
