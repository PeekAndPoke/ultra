package de.peekandpoke.kraft.examples.jsaddons.chartjs

import de.peekandpoke.kraft.addons.chartjs.ChartJs
import de.peekandpoke.kraft.addons.chartjs.chartJsData
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.streams.addons.map
import de.peekandpoke.kraft.streams.addons.ticker
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.height
import kotlinx.css.vh
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@Suppress("FunctionName")
fun Tag.ChartJsExample() = comp {
    ChartJsExample(it)
}

class ChartJsExample(ctx: NoProps) : PureComponent(ctx) {

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val factor by subscribingTo(ticker(1.seconds).map { 1.0 + sin(it * 10 * PI / 180.0) })

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {

            ui.header H2 { +"Chart.js demos" }

            ui.two.cards {
                ui.card {
                    ui.content {
                        css { height = 50.vh }
                        simpleBarChart()
                    }
                    ui.content {
                        ui.header { +"Bar Chart" }
                        ui.meta {
                            a(
                                href = "https://www.chartjs.org/docs/latest/samples/bar/vertical.html",
                                target = "_blank"
                            ) {
                                +"Docs"
                            }
                        }
                    }
                }

                ui.card {
                    ui.content {
                        css { height = 50.vh }
                        simpleLineChart()
                    }
                    ui.content {
                        ui.header { +"Line Chart" }
                        ui.meta {
                            a(href = "https://www.chartjs.org/docs/latest/samples/line/line.html", target = "_blank") {
                                +"Docs"
                            }
                        }
                    }
                }

                ui.card {
                    ui.content {
                        css { height = 50.vh }
                        simpleDoughnutChart()
                    }
                    ui.content {
                        ui.header { +"Doughnut Chart" }
                        ui.meta {
                            a(
                                href = "https://www.chartjs.org/docs/latest/samples/other-charts/doughnut.html",
                                target = "_blank"
                            ) {
                                +"Docs"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.simpleBarChart() {
        val data = chartJsData {
            jsObject {
                type = "bar"
                data = jsObject {
                    labels = arrayOf("Red", "Blue", "Yellow", "Green", "Purple", "Orange")
                    datasets = arrayOf(
                        jsObject {
                            label = "# of votes"
                            data = arrayOf(12, 19, 3, 5, 2, 3).map { it * factor }.toTypedArray()
                            color = value("#444")
                            backgroundColor = value(
                                "rgba(255, 99, 132, 0.2)",
                                "rgba(54, 162, 235, 0.2)",
                                "rgba(255, 206, 86, 0.2)",
                                "rgba(75, 192, 192, 0.2)",
                                "rgba(153, 102, 255, 0.2)",
                                "rgba(255, 159, 64, 0.2)",
                            )
                            borderColor = value(
                                "rgba(255, 99, 132, 1)",
                                "rgba(54, 162, 235, 1)",
                                "rgba(255, 206, 86, 1)",
                                "rgba(75, 192, 192, 1)",
                                "rgba(153, 102, 255, 1)",
                                "rgba(255, 159, 64, 1)",
                            )
                            borderWidth = value(1)
                        }
                    )
                }

                options = jsObject {
                    scales = jsObject {
                        yAxes = jsObject {
                            ticks = jsObject {
                                suggestedMin = 0
                                suggestedMax = 40
                            }
                        }
                    }
                }
            }
        }

        ChartJs(data)
    }

    private fun FlowContent.simpleLineChart() {
        val data = chartJsData {
            jsObject {
                type = "line"
                data = jsObject {
                    labels = numberLabels(6)
                    datasets = arrayOf(
                        jsObject {
                            label = "# of votes"
                            data = arrayOf(12, 19, 3, 5, 2, 3).map { it * factor }.toTypedArray()
                            color = value("#444")
                            backgroundColor = value("rgba(255, 99, 132, 0.2)")
                            borderColor = value("rgba(255, 99, 132, 1)")
                            borderWidth = value(3)
                        }
                    )
                }

                options = jsObject {
                    scales = jsObject {
                        yAxes = jsObject {
                            ticks = jsObject {
                                suggestedMin = 0
                                suggestedMax = 40
                            }
                        }
                    }
                }
            }
        }

        ChartJs(data)
    }

    private fun FlowContent.simpleDoughnutChart() {
        val data = chartJsData {
            jsObject {
                type = "doughnut"
                data = jsObject {
                    labels = numberLabels(6)
                    datasets = arrayOf(
                        jsObject {
                            label = "Dataset #1"
                            data = arrayOf(12, 19, 3, 5, 2, 3)
                                .mapIndexed { index, it -> it * (index + 1).toDouble().pow(factor) }
                                .toTypedArray()
                            backgroundColor = value(
                                "rgba(255, 99, 132, 0.2)",
                                "rgba(54, 162, 235, 0.2)",
                                "rgba(255, 206, 86, 0.2)",
                                "rgba(75, 192, 192, 0.2)",
                                "rgba(153, 102, 255, 0.2)",
                                "rgba(255, 159, 64, 0.2)",
                            )
                        }
                    )
                }
            }
        }

        ChartJs(data)
    }
}
