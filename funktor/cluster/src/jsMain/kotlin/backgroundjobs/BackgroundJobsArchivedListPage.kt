package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.addons.semanticui.PaginationPages.PaginationPages
import de.peekandpoke.kraft.addons.semanticui.pagination.PaginationEpp
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
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
        val ui: FunktorClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var filter: PagedSearchFilter by pagedSearchFilter(router = props.ui.router) { loader.reloadSilently() }

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
                    th {
                        +"Id"
                    }
                    th {
                        +"Type"
                    }
                    th {
                        +"Created At"
                    }
                    th {
                        +"Archived At"
                    }
                    th {
                        +"Data"
                    }
                    th {
                        +"Retry Policy"
                    }
                    th {
                        +"Last Execution Time"
                    }
                    th {
                        +"Results"
                    }
                }
            }
            tbody {
                jobs.forEach { job ->

                    tr("top aligned") {

                        classes = classes + when {
                            job.lastTryDidSucceed() -> "positive"
                            job.lastTryDidFail() -> "negative"
                            else -> "warning"
                        }

                        onClick { event ->
                            props.ui.navTo(event) {
                                backgroundJobs.viewArchived(id = job.id)
                            }
                        }

                        td { // Id
                            +job.id
                        }
                        td { // Type
                            +job.type
                        }
                        td { // Created at
                            +(job.createdAt?.atSystemDefaultZone()?.formatDdMmmYyyyHhMmSs() ?: "n/a")
                        }
                        td { // Archived at
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
                        td { // Last Execution Time
                            when (val last = job.results.lastOrNull()) {
                                null -> +"n/a"
                                else -> +"${last.executionTimeMs}ms"
                            }
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
