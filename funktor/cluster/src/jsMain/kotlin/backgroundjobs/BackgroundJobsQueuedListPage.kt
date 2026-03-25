@file:Suppress("detekt:LongMethod")

package io.peekandpoke.funktor.cluster.backgroundjobs

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.pagination.PaginationEpp
import io.peekandpoke.kraft.semanticui.pagination.PaginationPages
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
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

class BackgroundJobsQueuedListPage(ctx: Ctx<Props>) : Component<BackgroundJobsQueuedListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var filter: PagedSearchFilter by pagedSearchFilter { loader.reloadSilently() }

    private val loader = dataLoader {
        props.ui.api.backgroundJobs
            .listQueued(page = filter.page, epp = filter.epp)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Background Jobs Queue") }

        ui.padded.segment {

            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Background Jobs Queue" }
            }

            ui.hidden.divider {}

            loader.renderDefault(this) { jobs ->
                renderPagination(jobs)

                renderTable(jobs.items)

                renderPagination(jobs)
            }
        }
    }

    fun FlowContent.renderTable(jobs: List<BackgroundJobQueuedModel>) {

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
                        +"Due At"
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

                        if (job.lastTryDidFail()) {
                            classes = classes + "negative"
                        }

                        onClick { event ->
                            router.navToUri(
                                evt = event,
                                route = props.ui.routes.backgroundJobs.viewQueued(id = job.id)
                            )
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
                        td { // Due at
                            +job.dueAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
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
                                else -> +"${last.executionDurationMs} ms"
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

    private fun FlowContent.renderPagination(jobs: Paged<BackgroundJobQueuedModel>) {
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
