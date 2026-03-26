@file:JsModule("chart.js")
@file:JsNonModule

package io.peekandpoke.kraft.addons.chartjs

import org.w3c.dom.CanvasRenderingContext2D

/** All registerable Chart.js modules (controllers, scales, elements, plugins). */
external val registerables: Array<Any>

/** External binding for the Chart.js `Chart` class. */
external class Chart(
    ctx: CanvasRenderingContext2D,
    data: dynamic,
) {
    companion object {
        /** Chart.js built-in helper utilities. */
        val helpers: dynamic

        /** Registers Chart.js modules globally. */
        fun register(vararg registerables: Any)
    }

    var data: ChartConfig.ChartData
    var options: ChartConfig.Options

    /** Updates the chart, optionally with an animation [duration] in milliseconds. */
    fun update(duration: Number? = definedExternally)

    /** Destroys the chart instance and releases resources. */
    fun destroy()
}
