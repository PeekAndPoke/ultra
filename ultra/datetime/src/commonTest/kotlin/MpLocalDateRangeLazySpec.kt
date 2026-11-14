package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.ComputedList

/** ULTRA-00341 batch 3b-1: `asListOfDates()` returns a lazy view; no cap, no exception. */
@Suppress("unused")
class MpLocalDateRangeLazySpec : StringSpec({

    "forever.asListOfDates() is a lazy view of 3 652 058 dates, cheap to create and to read at its ends" {
        val dates = MpLocalDateRange.forever.asListOfDates()

        (dates is ComputedList<*>) shouldBe true
        (dates is RandomAccess) shouldBe true
        dates.size shouldBe 3_652_058
        dates.first() shouldBe MpLocalDate.of(1, 1, 1)
        dates.last() shouldBe MpLocalDate.of(9999, 12, 30)
        dates[1_000_000] shouldBe MpLocalDate.of(2738, 11, 29)
    }

    "MpClosedLocalDateRange.forever: the same kind of view, through Doomsday (9999-12-31) itself" {
        val dates = MpClosedLocalDateRange.forever.asListOfDates()

        dates.size shouldBe 3_652_059
        dates.last() shouldBe MpLocalDate.of(9999, 12, 31)
    }

    "a small range gives the same dates as before" {
        val range = MpLocalDateRange(MpLocalDate.of(2024, 2, 27), MpLocalDate.of(2024, 3, 2))

        range.asListOfDates() shouldBe listOf(
            MpLocalDate.of(2024, 2, 27), MpLocalDate.of(2024, 2, 28),
            MpLocalDate.of(2024, 2, 29), MpLocalDate.of(2024, 3, 1),
        )
        MpClosedLocalDateRange(MpLocalDate.of(2024, 2, 28), MpLocalDate.of(2024, 3, 1)).asListOfDates() shouldBe listOf(
            MpLocalDate.of(2024, 2, 28), MpLocalDate.of(2024, 2, 29), MpLocalDate.of(2024, 3, 1),
        )
    }

    "an invalid range gives an empty list, as before" {
        MpLocalDateRange(MpLocalDate.of(2024, 3, 2), MpLocalDate.of(2024, 2, 27)).asListOfDates() shouldBe emptyList()
        MpLocalDateRange(MpLocalDate.of(2024, 3, 2), MpLocalDate.of(2024, 3, 2)).asListOfDates() shouldBe emptyList()
    }
})
