package io.peekandpoke.kraft.addons.chartjs

/**
 * Type declarations for Chart.js.
 *
 * Instances are created via [ChartJsAddon.createChart].
 * The JS library is loaded dynamically — no static @JsModule import.
 */
external class Chart {
    var data: ChartConfig.ChartData
    var options: ChartConfig.Options

    /** Updates the chart, optionally with an animation [duration] in milliseconds. */
    fun update(duration: Number? = definedExternally)

    /** Destroys the chart instance and releases resources. */
    fun destroy()
}
