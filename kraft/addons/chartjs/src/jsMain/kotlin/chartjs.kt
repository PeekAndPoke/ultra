package io.peekandpoke.kraft.addons.chartjs

/**
 * Type declarations for Chart.js.
 *
 * Instances are created via [ChartJsAddon.createChart].
 * The JS library is loaded dynamically — no static @JsModule import.
 */
external class Chart {
    /** The live configuration of the chart, e.g. its [ChartConfig.type]. */
    val config: ChartConfig
    var data: ChartConfig.ChartData
    var options: ChartConfig.Options

    /**
     * Redraws the chart after its [data] or [options] changed.
     *
     * The optional [mode] is one of the Chart.js 4 update modes: "default", "none" (no animation), "active",
     * "hide", "show", "reset", "resize".
     */
    fun update(mode: String? = definedExternally)

    /** Destroys the chart instance and releases resources. */
    fun destroy()
}
