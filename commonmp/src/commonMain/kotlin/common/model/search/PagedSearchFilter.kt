package de.peekandpoke.ultra.common.model.search

import de.peekandpoke.ultra.common.safeEnumValueOrNull
import de.peekandpoke.ultra.common.toggle
import kotlinx.serialization.Serializable

@Serializable
data class PagedSearchFilter(
    val search: String = "",
    val page: Int = defaultPage,
    val epp: Int = defaultEpp,
    val options: Set<Option> = emptySet(),
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
            options: String? = "",
        ) = PagedSearchFilter(
            search = search,
            page = page,
            epp = epp,
            options = options.toOptions(),
        )

        /**
         * Constructs a [PagedSearchFilter] from the given [map].
         */
        fun ofMap(map: Map<String, String?>) = PagedSearchFilter(
            search = map["search"] ?: "",
            page = map["page"]?.toIntOrNull() ?: defaultPage,
            epp = map["epp"]?.toIntOrNull() ?: defaultEpp,
            options = map["options"].toOptions(),
        )

        private fun String?.toOptions(): Set<Option> = this
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { option -> safeEnumValueOrNull<Option>(option) }
            ?.toSet()
            ?: emptySet()
    }

    @Suppress("EnumEntryName")
    enum class Option {
        include_deleted,
        include_recently_deleted,
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
     * Split the [options] string and trying at "," and to convert each part to an [Option].
     */
    fun withOptions(options: String) = copy(
        options = options.toOptions(),
    )

    /**
     * Add an [option].
     */
    fun plusOption(option: Option) = copy(
        options = options.plus(option)
    )

    /**
     * Removes an [option].
     */
    fun minusOption(option: Option) = copy(
        options = options.minus(option)
    )

    /**
     * Toggles an [option].
     */
    fun toggleOption(option: Option) = copy(
        options = options.toggle(option)
    )

    /**
     * Returns 'true' when the [Option.include_deleted] is set
     */
    fun hasIncludeDeletedOption() = options.contains(Option.include_deleted)

    /**
     * Returns 'true' when the [Option.include_recently_deleted] is set
     */
    fun hasIncludeRecentlyDeletedOption() = options.contains(Option.include_recently_deleted)

    /**
     * Converts the filter to a [Map].
     */
    fun toMap(): Map<String, String> {
        return mapOf(
            "search" to search.trim(),
            "page" to page.toString(),
            "epp" to epp.toString(),
            "options" to options.joinToString(",") { it.name },
        )
    }

    fun toEntries(): Array<Pair<String, String>> {
        @Suppress("UNCHECKED_CAST")
        return toMap().entries
            .map { it.key to it.value }
            .toTypedArray()
    }
}
