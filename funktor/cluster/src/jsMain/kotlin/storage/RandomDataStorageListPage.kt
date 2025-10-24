package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.semanticui.forms.UiInputField
import de.peekandpoke.kraft.semanticui.pagination.PaginationEpp
import de.peekandpoke.kraft.semanticui.pagination.PaginationPages
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
import de.peekandpoke.ultra.common.roundWithPrecision
import de.peekandpoke.ultra.html.key
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class RandomDataStorageListPage(ctx: Ctx<Props>) : Component<RandomDataStorageListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    var filter: PagedSearchFilter by pagedSearchFilter { loader.reloadSilently() }

    private val loader = dataLoader {
        props.ui.api.storage.randomData
            .list(search = filter.search, page = filter.page, epp = filter.epp)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Random Storage") }

        ui.padded.segment {

            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Random Data Storage" }
            }

            ui.top.attached.segment {
                ui.form {
                    UiInputField(filter.search, { filter = filter.copy(search = it, page = 1) }) {
                        placeholder("Search")
                    }
                }
            }

            loader.renderDefault(this) { entries ->
                renderPaginationAsAttachedSegment(entries, filter) { filter = it.copy(page = 1) }

                renderTable(entries.items)

                renderPaginationAsAttachedSegment(entries, filter) { filter = it.copy(page = 1) }
            }
        }
    }

    private fun FlowContent.renderTable(entries: List<RawRandomDataModel.Head>) {
        ui.attached.striped.selectable.table Table {
            thead {
                tr {
                    th {
                        +"Category"
                    }
                    th {
                        +"Data Id"
                    }
                    th {
                        +"Size"
                    }
                    th {
                        +"Created"
                    }
                    th {
                        +"Updated"
                    }
                }
            }
            tbody {
                entries.forEach { item ->

                    tr {
                        onClick { event ->
                            router.navToUri(
                                evt = event,
                                route = props.ui.routes.storage.randomData.view(item.id)
                            )
                        }

                        td { // Category
                            +item.category
                        }
                        td { // Data id
                            +item.dataId
                        }
                        td { // Size
                            when {
                                item.size < 10_000 -> +"~ ${item.size} bytes"
                                item.size < 1_000_000 -> +"~ ${(item.size / 1024).roundWithPrecision(0)} kb"
                                else -> +"~ ${(item.size / (1024 * 1024)).roundWithPrecision(0)} Mb"
                            }
                        }
                        td { // Created
                            +item.createdAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                        td { // Updated
                            +item.updatedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                        }
                    }
                }
            }
        }
    }

    // the helper is not resetting the page=1
    fun <T> FlowContent.renderPaginationAsAttachedSegment(
        paged: Paged<T>,
        filter: PagedSearchFilter,
        onChange: (PagedSearchFilter) -> Unit,
    ) {
        ui.attached.segment {
            key = "page-${filter.page}-epp-${filter.epp}"

            ui.horizontal.list {
                ui.item {
                    PaginationPages(
                        activePage = filter.page,
                        totalPages = paged.fullPageCount
                    ) { onChange(filter.copy(page = it)) }
                }
                ui.item {
                    PaginationEpp(epp = filter.epp) { onChange(filter.copy(epp = it, page = 1)) }
                }
            }
        }
    }
}
