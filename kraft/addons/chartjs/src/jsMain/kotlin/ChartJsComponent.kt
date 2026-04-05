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

/** Renders a Chart.js chart from the given [data] configuration. */
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

            onUnmount {
                chart?.destroy()
                chart = null
            }
        }
    }

    override fun shouldRedraw(nextProps: Props): Boolean {
        chart?.let { c ->
            try {
                // Update the options
                c.options = nextProps.data.options

                // Merge data sets
                val currentData = c.data.datasets
                val nextData = nextProps.data.data.datasets

                nextData.forEachIndexed { sid, dataset ->

                    val current = currentData.getOrNull(sid)

                    if (current != null) {
                        current.data = dataset.data
                    } else {
                        currentData[sid].data = dataset.data
                    }
                }

                c.update()
            } catch (e: Throwable) {
                console.error("Updating the chart failed")
                console.error(e)
            }
        }

        return super.shouldRedraw(nextProps)
    }

    override fun VDom.render() {
        canvas {
            style = "width: 100%; height: 100%;"
        }
    }

    private fun tryCreateChart() {
        val addon = chartJsAddon ?: return
        if (chart != null) return

        val canvas = dom as? HTMLCanvasElement ?: return

        addon.registerAll()

        chart = addon.createChart(
            ctx = canvas.getContext("2d") as CanvasRenderingContext2D,
            config = props.data,
        )
    }
}
