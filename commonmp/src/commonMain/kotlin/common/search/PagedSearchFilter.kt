package de.peekandpoke.ultra.common.search

import kotlinx.serialization.Serializable

@Serializable
data class PagedSearchFilter(
    val search: String,
    val page: Int = 0,
    val epp: Int = 20,
)
