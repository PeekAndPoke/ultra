package de.peekandpoke.kraft.addons.chartjs

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.canvas
import kotlinx.html.style
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

@Suppress("FunctionName")
fun Tag.ChartJs(
    data: ChartConfig,
) = comp(
    ChartJsComponent.Props(data = data)
) {
    ChartJsComponent(it)
}

class ChartJsComponent(ctx: Ctx<Props>) : Component<ChartJsComponent.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val data: ChartConfig,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var chart: Chart? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        ChartJsUtils.registerAllModules()

        lifecycle {
            onMount {
                val canvas = dom as HTMLCanvasElement
                // console.log("chart-data", props.data)

                chart = Chart(
                    ctx = canvas.getContext("2d") as CanvasRenderingContext2D,
                    data = props.data,
                )
            }

            onUnmount {
                chart?.destroy()
                chart = null
            }
        }
    }

    override fun shouldRedraw(nextProps: Props): Boolean {
//        return super.shouldRedraw(nextProps)
        chart?.let { c ->
            try {
//                console.log(Chart)
//                console.log(Object.getOwnPropertyNames(Chart))

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
}
