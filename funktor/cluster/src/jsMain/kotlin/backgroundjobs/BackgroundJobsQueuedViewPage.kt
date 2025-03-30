package de.peekandpoke.ktorfx.cluster.backgroundjobs

import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.streams.addons.debouncedFuncExceptFirst
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ktorfx.cluster.KtorFxClusterUi
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import kotlinx.html.FlowContent

class BackgroundJobsQueuedViewPage(ctx: Ctx<Props>) : Component<BackgroundJobsQueuedViewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: KtorFxClusterUi,
        val id: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var job: BackgroundJobQueuedModel? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private val reload = debouncedFuncExceptFirst {
        launch {
            props.ui.api.backgroundJobs
                .getQueued(id = props.id)
                .collect {
                    job = it.data!!
                }
        }
    }

    init {
        reload()
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("KtorFx", "Background Jobs Queue", props.id) }

        ui.padded.segment {
            ui.dividing.header H2 { +"Queued Background Job '${props.id}'" }

            when (val loaded = job) {
                null -> ui.active.inline.loader {}
                else -> renderContent(loaded)
            }
        }
    }

    private fun FlowContent.renderContent(loaded: BackgroundJobQueuedModel) {
        ui.cards {
            ui.card {
                noui.content {
                    noui.header { +"Last Result" }
                }
                noui.content {
                    renderIcon(loaded.results.lastOrNull()) { big }
                }
            }
            ui.card {
                noui.content {
                    noui.header { +"Type" }
                }
                noui.content {
                    +loaded.type
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"Created At" }
                }
                noui.content {
                    +(loaded.createdAt?.atSystemDefaultZone()?.formatDdMmmYyyyHhMmSs() ?: "n/a")
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"Due At" }
                }
                noui.content {
                    +loaded.dueAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                }
            }

            ui.card {
                noui.content {
                    noui.header { +"Retry Policy" }
                }
                noui.content {
                    renderTableEntry(loaded.retryPolicy)
                }
            }
        }

        ui.dividing.header { +"Data" }

        renderJobData(loaded.data, loaded.dataHash)

        ui.dividing.header { +"Results" }

        renderResultsAsSegments(loaded.results.asReversed())
    }
}
