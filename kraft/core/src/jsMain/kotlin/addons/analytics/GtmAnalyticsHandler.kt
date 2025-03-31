package de.peekandpoke.kraft.addons.analytics

import de.peekandpoke.kraft.utils.jsObjectOf
import kotlinx.browser.window

class GtmAnalyticsHandler(
    val eventPrefix: String = "",
    val defaultParameters: Map<String, String> = emptyMap(),
    val dataLayerName: String = "dataLayer",
) : AnalyticsHandler {

    private val dataLayer: dynamic get() = window.asDynamic()[dataLayerName]

    override fun sendEvent(eventName: String, parameters: Map<String, Any>) {

        val dl = dataLayer

        if (dl != undefined) {

            val data = jsObjectOf(
                *defaultParameters.entries.map { it.key to it.value }.toTypedArray(),
                *parameters.entries.map { it.key to it.value }.toTypedArray(),
                "event" to eventPrefix + eventName,
            )

            dl.push(data)
        }
    }
}
