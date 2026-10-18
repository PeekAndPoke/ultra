package io.peekandpoke.kraft.addons.chartjs

import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.style
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

/**
 * Renders a Chart.js chart from the given [data] configuration.
 *
 * A new [data] updates the live chart: options, the data fields (labels, ...) and the datasets follow it. Datasets
 * are matched by index, so per-series state such as legend visibility stays with the position. A changed
 * [ChartConfig.type] rebuilds the chart. Inline `plugins` are read only when the chart is created.
 *
 * Pass a new [ChartConfig] for every change. The chart works on a one-level copy: point objects and nested option
 * objects stay shared, so changing them in place shows up only at some later update, and Chart.js may annotate
 * points (`_stacks` with `parsing: false` and stacking). Dataset fields starting with "_" are reserved for Chart.js
 * and not passed through.
 *
 * The options are replaced, not merged, so a chart updated many times per second sets `options.animation = false`
 * in every config it passes.
 */
@Suppress("FunctionName")
fun Tag.ChartJs(
    data: ChartConfig,
) = comp(
    ChartJsComponent.Props(data = data)
) {
    ChartJsComponent(it)
}

/** Kraft component that wraps a Chart.js canvas and manages the chart lifecycle. */
class ChartJsComponent(ctx: Ctx<Props>) : Component<ChartJsComponent.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val data: ChartConfig,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val chartJsAddon: ChartJsAddon? by subscribingTo(addons.chartJs)

    private var chart: Chart? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                tryCreateChart()
            }

            onUpdate {
                tryCreateChart()
            }

            onNextProps { new, _ ->
                updateChart(new.data)
            }

            onUnmount {
                destroyChart()
            }
        }
    }

    override fun VDom.render() {
        canvas {
            style = "width: 100%; height: 100%;"
        }
    }

    private fun updateChart(next: ChartConfig) {
        val c = chart ?: return

        try {
            if (c.config.type != next.type) {
                // A fresh chart rather than a type switch: clean state, no animation across chart types
                destroyChart()
                tryCreateChart()
            } else {
                c.mergeFrom(next)
                c.update()
            }
        } catch (e: Throwable) {
            console.error("Updating the chart failed")
            console.error(e)
        }
    }

    private fun destroyChart() {
        chart?.destroy()
        chart = null
    }

    private fun tryCreateChart() {
        val addon = chartJsAddon ?: return
        if (chart != null) return

        val canvas = dom as? HTMLCanvasElement ?: return

        addon.registerAll()

        try {
            chart = addon.createChart(
                ctx = canvas.getContext("2d") as CanvasRenderingContext2D,
                config = ownedCopy(props.data),
            )
        } catch (e: Throwable) {
            // Chart.js registers the chart on the canvas before its first update can throw
            addon.getChart(canvas)?.destroy()

            console.error("Creating the chart failed")
            console.error(e)
        }
    }
}
