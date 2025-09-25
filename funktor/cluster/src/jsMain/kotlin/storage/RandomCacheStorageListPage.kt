package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.pagination.pagedSearchFilter
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.pagination.PaginationEpp
import de.peekandpoke.kraft.addons.semanticui.pagination.PaginationPages
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.model.Paged
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
import de.peekandpoke.ultra.common.roundWithPrecision
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

class RandomCacheStorageListPage(ctx: Ctx<Props>) : Component<RandomCacheStorageListPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    var filter: PagedSearchFilter by pagedSearchFilter(router = props.ui.router) { loader.reloadSilently() }

    private val loader = dataLoader {
        props.ui.api.storage.randomCache
            .list(search = filter.search, page = filter.page, epp = filter.epp)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////
    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Random Cache") }

        ui.padded.segment {

            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Random Cache Storage" }
            }

            ui.top.attached.segment {
                ui.form {
                    UiInputField(filter.search, { filter = filter.copy(search = it, page = 1) }) {
                        placeholder("Search")
                    }
                }
            }

            loader.renderDefault(this) { entries ->
                renderPaginationAsAttachedSegment(entries, filter) { filter = it }

                renderTable(entries.items)

                renderPaginationAsAttachedSegment(entries, filter) { filter = it }
            }
        }
    }

    private fun FlowContent.renderTable(entries: List<RawCacheDataModel.Head>) {
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
                        +"Policy"
                    }
                    th {
                        +"Timestamps"
                    }
                }
            }
            tbody {
                entries.forEach { item ->

                    tr("top aligned") {
                        onClick { event ->
                            props.ui.navTo(event) {
                                storage.randomCache.view(item.id)
                            }
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
                        td { // Policy
                            item.renderPolicyAsList(this)
                        }
                        td { // Timestamps
                            item.renderTimestampsAsList(this)
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
