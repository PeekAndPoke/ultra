@file:JsModule("chart.js")
@file:JsNonModule

package de.peekandpoke.kraft.addons.chartjs

import org.w3c.dom.CanvasRenderingContext2D

external val registerables: Array<Any>

external class Chart(
    ctx: CanvasRenderingContext2D,
    data: dynamic,
) {
    companion object {
        val helpers: dynamic
        fun register(vararg registerables: Any)
    }

    var data: ChartConfig.ChartData
    var options: ChartConfig.Options

    fun update(duration: Number? = definedExternally)
    fun destroy()
}
