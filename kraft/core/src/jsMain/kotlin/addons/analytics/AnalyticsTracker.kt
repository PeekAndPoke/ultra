package de.peekandpoke.kraft.addons.analytics

data class AnalyticsTracker(private val handlers: List<AnalyticsHandler>) {

    fun sendEvent(eventName: String, vararg parameters: Pair<String, Any>) {
        sendEvent(eventName, parameters.toMap())
    }

    fun sendEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {

        handlers.forEach {
            try {
                it.sendEvent(eventName, parameters)
            } catch (e: Throwable) {
                console.error("Error tracking event with ${it::class.simpleName}", e)
            }
        }
    }
}
