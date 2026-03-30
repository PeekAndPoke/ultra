package io.peekandpoke.funktor.inspect.cluster.backgroundjobs

import io.peekandpoke.funktor.inspect.cluster.FunktorInspectClusterUi
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.pagination.PaginationEpp
import io.peekandpoke.kraft.semanticui.pagination.PaginationPages
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.common.roundWithPrecision
import io.peekandpoke.ultra.datetime.formatDdMmmYyyyHhMmSs
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.classes
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.pre
import kotlinx.html.summary
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class BackgroundJobsArchivedListPage(ctx: Ctx<Props>) : Component<BackgroundJobsArchivedListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorInspectClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var filter: PagedSearchFilter by pagedSearchFilter { loader.reloadSilently() }

    private val loader = dataLoader {
        props.ui.api.backgroundJobs
            .listArchived(page = filter.page, epp = filter.epp)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Background Jobs Archive") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Background Jobs Archive" }
            }

            ui.hidden.divider {}

            loader.renderDefault(this) { jobs ->
                renderPagination(jobs)

                renderTable(jobs.items)

                renderPagination(jobs)
            }
        }
    }

    private fun FlowContent.renderTable(jobs: List<BackgroundJobArchivedModel>) {
        ui.attached.striped.selectable.table Table {
            thead {
                tr {
                    th { +"Id" }
                    th { +"Type" }
                    th { +"Created" }
                    th { +"Archived" }
                    th { +"Data" }
                    th { +"Retry Policy" }
                    th { +"Time" }
                    th { +"CPU" }
                    th { +"Results" }
                }
            }
            tbody {
                jobs.forEach { job ->

                    val lastResult = job.results.lastOrNull()

                    tr("top aligned") {

                        classes = classes + when {
                            job.lastTryDidSucceed() -> "positive"
                            job.lastTryDidFail() -> "negative"
                            else -> "warning"
                        }

                        onClick { event ->
                            router.navToUri(
                                evt = event,
                                route = props.ui.routes.backgroundJobs.viewArchived(id = job.id)
                            )
                        }

                        td { // Id
                            +job.id
                        }
                        td { // Type
                            +job.type
                        }
                        td { // Created
                            +(job.createdAt?.atSystemDefaultZone()?.formatDdMmmYyyyHhMmSs() ?: "n/a")
                        }
                        td { // Archived
                            +job.archivedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                        td { // Data
                            details {
                                onClick { evt -> evt.stopPropagation() }
                                summary {
                                    +"Hash: ${job.dataHash}"
                                }
                                pre {
                                    +job.data
                                }
                            }
                        }
                        td { // Retry Policy
                            renderTableEntry(job.retryPolicy)
                        }
                        td { // Time
                            when (lastResult) {
                                null -> +"n/a"
                                else -> +"${lastResult.executionDurationMs} ms"
                            }
                        }
                        td { // CPU
                            val cpu = lastResult?.cpuProfile
                                ?.let { "${it.totalCpuUsagePct.roundWithPrecision(2)} %" }

                            +(cpu ?: "n/a")
                        }
                        td { // Results
                            renderTableEntry(job.results)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderPagination(jobs: Paged<BackgroundJobArchivedModel>) {
        ui.attached.segment {
            ui.horizontal.list {
                ui.item {
                    PaginationPages(activePage = filter.page, totalPages = jobs.fullPageCount) {
                        filter = filter.copy(page = it)
                    }
                }
                ui.item {
                    PaginationEpp(epp = filter.epp) {
                        filter = filter.copy(epp = it, page = 1)
                    }
                }
            }
        }
    }
}
