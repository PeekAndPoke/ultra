package de.peekandpoke.ultra.common.model.search

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PagedSearchFilterSpec : StringSpec({

    "Empty filter round trip" {

        val original = PagedSearchFilter()

        val data = original.toMap()

        val result = PagedSearchFilter.ofMap(data)

        result shouldBe original
    }

    "Fully configured round trip" {
        val original = PagedSearchFilter(
            search = "search",
            page = 2,
            epp = 100,
            searchOptions = setOf(PagedSearchFilter.Option.include_deleted),
        )

        val data = original.toMap()

        val result = PagedSearchFilter.ofMap(data)

        result shouldBe original
    }

    "creating using ofMap() from empty data" {
        val result = PagedSearchFilter.ofMap(emptyMap())

        result shouldBe PagedSearchFilter(
            search = "",
            page = 1,
            epp = 20,
            searchOptions = emptySet(),
        )
    }

    "creating using of()" {

        val result = PagedSearchFilter.of(
            search = "search",
            page = 2,
            epp = 100,
            searchOptions = "include_deleted",
        )

        result shouldBe PagedSearchFilter(
            search = "search",
            page = 2,
            epp = 100,
        ).withOptions(PagedSearchFilter.Option.include_deleted.name)
    }
})
