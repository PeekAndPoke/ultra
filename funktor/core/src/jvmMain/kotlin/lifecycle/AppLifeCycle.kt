package io.peekandpoke.funktor.core.lifecycle

import io.ktor.events.EventDefinition
import io.ktor.events.EventHandler
import io.ktor.server.application.*

internal class AppLifeCycle(val app: Application) {

    class EventAndHandler<T>(
        val event: EventDefinition<T>,
        val handler: EventHandler<T>,
    )

    private var registeredListeners = listOf<EventAndHandler<out Any?>>()
    private var cleanupInstalled = false

    fun register(listeners: List<EventAndHandler<out Any?>>) {

        val toAdd = if (!cleanupInstalled) {
            cleanupInstalled = true
            listeners + EventAndHandler(ApplicationStopped) {
                it.log.info("LifeCycle: cleaning up")
                unsubscribeAll()
            }
        } else {
            listeners
        }

        toAdd.forEach {
            @Suppress("UNCHECKED_CAST")
            app.monitor.subscribe(it.event, it.handler as EventHandler<Any?>)
        }

        registeredListeners = registeredListeners + toAdd

        app.log.info("LifeCycle: added ${toAdd.size} listeners (total: ${registeredListeners.size})")
    }

    private fun unsubscribeAll() {

        app.log.info("LifeCycle: unsubscribing ${registeredListeners.size} listeners")

        registeredListeners.forEach {
            @Suppress("UNCHECKED_CAST")
            app.monitor.unsubscribe(it.event, it.handler as EventHandler<Any?>)
        }

        registeredListeners = emptyList()
        cleanupInstalled = false
    }
}
