package de.peekandpoke.funktor.cluster.workers

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.formatNonZeroComponents
import de.peekandpoke.funktor.cluster.workers.api.WorkerModel
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.MpDateTimePeriod
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.pre
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class WorkerDetailsPage(ctx: Ctx<Props>) : Component<WorkerDetailsPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val id: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private fun now() = props.ui.kronos.instantNow()

    private var worker: WorkerModel? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch {
            worker = props.ui.api.workers
                .get(props.id)
                .first().data
        }
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Workers", props.id) }

        ui.padded.segment {
            ui.header H2 { +"Worker ${props.id}" }

            worker?.let {

                ui.striped.selectable.table Table {
                    thead {
                        tr {
                            th { +"Result" }
                            th { +"Started" }
                            th { +"Ended" }
                            th { +"Ago" }
                            th { +"Duration" }
                            th { +"Server" }
                            th { +"Details" }
                        }
                    }
                    tbody {
                        it.runs.forEach { run ->
                            tr {
                                td("collapsing") {
                                    renderResultIcon(run)
                                }
                                td("collapsing") { // Started
                                    +run.begin.atSystemDefaultZone().format("dd MMM yyyy HH:mm:ss.SSS")
                                }
                                td("collapsing") { // Ended
                                    +run.end.atSystemDefaultZone().format("dd MMM yyyy HH:mm:ss.SSS")
                                }
                                td {
                                    +MpDateTimePeriod.of(now() - run.end).formatNonZeroComponents()
                                }
                                td("collapsing") {
                                    +"${run.duration.inWholeMilliseconds} ms"
                                }
                                td("collapsing") {
                                    +run.serverId
                                }
                                td {
                                    renderDetails(run)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderResultIcon(run: WorkerModel.Run) {
        when (run.result) {
            is WorkerModel.Run.Result.Success -> icon.green.check_circle_outline()

            is WorkerModel.Run.Result.Failure -> icon.red.exclamation_triangle()
        }
    }

    private fun FlowContent.renderDetails(run: WorkerModel.Run) {
        when (val r = run.result) {
            is WorkerModel.Run.Result.Success -> {
            }

            is WorkerModel.Run.Result.Failure -> {
                ui.list {
                    ui.item {
                        ui.header { +r.message }
                        ui.description {
                            pre { +r.stack }
                        }
                    }
                }
            }
        }
    }
}
