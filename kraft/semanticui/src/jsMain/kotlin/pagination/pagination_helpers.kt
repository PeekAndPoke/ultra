package io.peekandpoke.kraft.semanticui.pagination

import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.model.Paged
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent

fun <T> FlowContent.renderPaginationAsAttachedSegment(
    paged: Paged<T>,
    filter: PagedSearchFilter,
    onChange: (PagedSearchFilter) -> Unit,
) {
    ui.attached.segment {
        renderPagination(paged, filter, onChange)
    }
}

fun <T> FlowContent.renderPagination(
    paged: Paged<T>,
    filter: PagedSearchFilter,
    onChange: (PagedSearchFilter) -> Unit,
) {
    ui.horizontal.list {
        key = "page-${filter.page}-epp-${filter.epp}"

        ui.item {
            PaginationPages(
                activePage = filter.page,
                totalPages = paged.fullPageCount
            ) { onChange(filter.copy(page = it)) }
        }
        ui.item {
            PaginationEpp(epp = filter.epp) { onChange(filter.copy(epp = it)) }
        }
    }
}
