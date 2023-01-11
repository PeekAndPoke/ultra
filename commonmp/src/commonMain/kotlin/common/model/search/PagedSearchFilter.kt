package de.peekandpoke.ultra.common.model.search

import de.peekandpoke.ultra.common.safeEnumValueOrNull
import kotlinx.serialization.Serializable

@Serializable
data class PagedSearchFilter(
    val search: String = "",
    val page: Int = defaultPage,
    val epp: Int = defaultEpp,
    val searchOptions: Set<Option> = emptySet(),
) {
    companion object {
        const val defaultPage = 1
        const val defaultEpp = 20

        fun of(
            search: String,
            page: Int,
            epp: Int,
            searchOptions: String,
        ) = PagedSearchFilter(
            search = search,
            page = page,
            epp = epp,
            searchOptions = searchOptions.toOptions(),
        )

        fun ofMap(map: Map<String, String?>) = PagedSearchFilter(
            search = map["search"] ?: "",
            page = map["page"]?.toIntOrNull() ?: defaultPage,
            epp = map["epp"]?.toIntOrNull() ?: defaultEpp,
            searchOptions = map["search-options"].toOptions(),
        )

        private fun String?.toOptions(): Set<Option> = this
            ?.split(",")
            ?.mapNotNull { option -> safeEnumValueOrNull<Option>(option) }
            ?.toSet()
            ?: emptySet()
    }

    @Suppress("EnumEntryName")
    enum class Option {
        include_deleted
    }

    fun withOptions(options: String) = copy(
        searchOptions = options.toOptions(),
    )

    fun toMap(): Map<String, String?> {
        return mapOf(
            "search" to if (search.isNotBlank()) {
                search.trim()
            } else {
                null
            },
            "page" to page.toString(),
            "epp" to epp.toString(),
            "search-options" to if (searchOptions.isNotEmpty()) {
                searchOptions.joinToString(",") { it.name }
            } else {
                null
            }
        )
    }

}
