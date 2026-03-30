package io.peekandpoke.funktor.inspect.cluster.workers

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.funktor.inspect.cluster.workers.api.WorkerModel
import io.peekandpoke.funktor.inspect.formatNonZeroComponents
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.common.roundWithPrecision
import io.peekandpoke.ultra.common.toFixed
import io.peekandpoke.ultra.datetime.MpDateTimePeriod
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.SemanticColor
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WorkersListPage(ctx: Ctx<Props>) : Component<WorkersListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorInspectClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun now() = props.ui.kronos.instantNow()

    private val loader = dataLoader {
        props.ui.api.workers.list().map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Workers") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Background Workers" }
            }

            loader.renderDefault(this) { data ->
                renderTable(data)
            }
        }
    }

    private fun FlowContent.renderTable(data: List<WorkerModel>) {
        ui.celled.striped.selectable.table Table {
            thead {
                tr {
                    th {
                        +"Status"
                    }
                    th {
                        +"Id"
                    }
                    th {
                        +"per hour"
                    }
                    th {
                        +"per min"
                    }
                    th {
                        +"per sec"
                    }
                    th {
                        +"Last start"
                    }
                    th {
                        +"Last end"
                    }
                    th {
                        +"Time ago"
                    }
                    th {
                        +"Last Duration"
                    }
                }
            }
            tbody {
                data.forEach { worker ->
                    val latest = worker.runs.firstOrNull()

                    tr {
                        onClick { event ->
                            router.navToUri(
                                evt = event,
                                route = props.ui.routes.workers.view(worker),
                            )
                        }

                        td {
                            when (latest) {
                                null -> +"n/a"
                                else -> {
                                    val successRate = worker.successRate

                                    val color = when {
                                        successRate >= 1.0 -> SemanticColor.green
                                        successRate >= 0.9 -> SemanticColor.yellow
                                        else -> SemanticColor.red
                                    }

                                    ui.color(color).label {
                                        +"${(successRate * 100).roundWithPrecision(2)} %"
                                    }
                                }
                            }
                        }

                        td { // Id
                            title = worker.id
                            +worker.shortenedId
                        }
                        td { // Throughput per hour
                            +"${worker.getRunsPer(1.hours).toFixed(2)} / h"
                        }
                        td { // Throughput per minute
                            +"${worker.getRunsPer(1.minutes).toFixed(2)} / min"
                        }
                        td { // Throughput per second
                            +"${worker.getRunsPer(1.seconds).toFixed(2)} / sec"
                        }
                        td {
                            +(latest?.begin?.atSystemDefaultZone()?.format("dd MMM yyyy HH:mm:ss.SSS") ?: "n/a")
                        }
                        td {
                            +(latest?.end?.atSystemDefaultZone()?.format("dd MMM yyyy HH:mm:ss.SSS") ?: "n/a")
                        }
                        td {
                            +(latest?.end?.let { MpDateTimePeriod.of(now() - it).formatNonZeroComponents() } ?: "n/a")
                        }
                        td {
                            val content = when (latest) {
                                null -> "n/a"
                                else -> "${latest.duration.inWholeMilliseconds} ms"
                            }
                            +content
                        }
                    }
                }
            }
        }
    }
}
