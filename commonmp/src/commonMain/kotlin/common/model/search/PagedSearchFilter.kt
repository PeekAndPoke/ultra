package de.peekandpoke.ultra.common.model.search

import de.peekandpoke.ultra.common.safeEnumValueOrNull
import de.peekandpoke.ultra.common.toggle
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

        /**
         * Constructs a [PagedSearchFilter] from the given parameter.
         */
        fun of(
            search: String = "",
            page: Int = defaultPage,
            epp: Int = defaultEpp,
            searchOptions: String? = "",
        ) = PagedSearchFilter(
            search = search,
            page = page,
            epp = epp,
            searchOptions = searchOptions.toOptions(),
        )

        /**
         * Constructs a [PagedSearchFilter] from the given [map].
         */
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

    /**
     * Set the [page] to 1 and [epp] to [Int.MAX_VALUE]
     */
    fun all() = copy(
        page = defaultPage,
        epp = Int.MAX_VALUE,
    )

    /**
     * Adds [options] from a single string
     *
     * Trie to split the  an [option]string and trying at "," and to convert each part to an [Option].
     */
    fun withOptions(options: String) = copy(
        searchOptions = options.toOptions(),
    )

    /**
     * Add an [option].
     */
    fun plusOption(option: Option) = copy(
        searchOptions = searchOptions.plus(option)
    )

    /**
     * Removes an [option].
     */
    fun minusOption(option: Option) = copy(
        searchOptions = searchOptions.minus(option)
    )

    /**
     * Toggles an [option].
     */
    fun toggleOption(option: Option) = copy(
        searchOptions = searchOptions.toggle(option)
    )

    /**
     * Converts the filter to a [Map].
     */
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
