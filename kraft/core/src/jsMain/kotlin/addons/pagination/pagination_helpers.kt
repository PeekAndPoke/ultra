package io.peekandpoke.kraft.addons.pagination

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.routing.urlParams
import io.peekandpoke.ultra.model.PagedSearchFilter
import kotlin.properties.ReadWriteProperty

fun <C> Component<C>.pagedSearchFilter(
    onChange: ((PagedSearchFilter) -> Unit)? = null,
): ReadWriteProperty<Component<C>, PagedSearchFilter> = urlParams(
    fromParams = { PagedSearchFilter.ofMap(it) },
    toParams = { it.toMap() },
    onChange = onChange,
)
