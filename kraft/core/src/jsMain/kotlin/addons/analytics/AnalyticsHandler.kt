package de.peekandpoke.kraft.addons.analytics

interface AnalyticsHandler {
    fun sendEvent(eventName: String, parameters: Map<String, Any>)
}
