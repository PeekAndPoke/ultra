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
            options = setOf(PagedSearchFilter.Option.include_deleted),
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
            options = emptySet(),
        )
    }

    "creating using of()" {

        val result = PagedSearchFilter.of(
            search = "search",
            page = 2,
            epp = 100,
            options = "include_deleted",
        )

        result shouldBe PagedSearchFilter(
            search = "search",
            page = 2,
            epp = 100,
        ).withOptions(PagedSearchFilter.Option.include_deleted.name)
    }

    "toMap" {

        PagedSearchFilter(
            page = 1,
            epp = 20,
        ).toMap() shouldBe mapOf(
            "search" to "",
            "page" to "1",
            "epp" to "20",
            "options" to "",
        )

        PagedSearchFilter(
            search = "search",
            page = 1,
            epp = 20,
            options = setOf(
                PagedSearchFilter.Option.include_deleted,
                PagedSearchFilter.Option.include_recently_deleted,
            )
        ).toMap() shouldBe mapOf(
            "search" to "search",
            "page" to "1",
            "epp" to "20",
            "options" to "include_deleted,include_recently_deleted"
        )
    }

    "toEntries" {

        PagedSearchFilter(
            page = 1,
            epp = 20,
        ).toEntries() shouldBe arrayOf(
            "search" to "",
            "page" to "1",
            "epp" to "20",
            "options" to "",
        )

        PagedSearchFilter(
            search = "search",
            page = 1,
            epp = 20,
            options = setOf(
                PagedSearchFilter.Option.include_deleted,
                PagedSearchFilter.Option.include_recently_deleted,
            )
        ).toEntries() shouldBe arrayOf(
            "search" to "search",
            "page" to "1",
            "epp" to "20",
            "options" to "include_deleted,include_recently_deleted"
        )
    }
})
