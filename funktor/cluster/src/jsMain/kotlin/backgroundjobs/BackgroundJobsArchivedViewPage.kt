package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import de.peekandpoke.ultra.streams.ops.debouncedFuncExceptFirst
import kotlinx.html.FlowContent

class BackgroundJobsArchivedViewPage(ctx: Ctx<Props>) : Component<BackgroundJobsArchivedViewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val id: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var job: BackgroundJobArchivedModel? by value(null)

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    private val reload = debouncedFuncExceptFirst {
        launch {
            props.ui.api.backgroundJobs
                .getArchived(id = props.id)
                .collect {
                    job = it.data!!
                }
        }
    }

    init {
        reload()
    }

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Background Jobs Archive", props.id) }

        ui.padded.segment {
            ui.dividing.header H2 { +"Archived Background Job '${props.id}'" }

            when (val loaded = job) {
                null -> ui.active.inline.loader {}
                else -> renderContent(loaded)
            }
        }
    }

    private fun FlowContent.renderContent(loaded: BackgroundJobArchivedModel) {
        ui.cards {
            ui.card {
                noui.content {
                    noui.header { +"Final Result" }
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
                    noui.header { +"Archived At" }
                }
                noui.content {
                    +loaded.archivedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
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
