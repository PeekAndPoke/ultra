package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.ComputedList

/** ULTRA-00341 batch 3b-1: `asListOfDates()` returns a lazy view; no cap, no exception. */
@Suppress("unused")
class MpLocalDateRangeLazySpec : StringSpec({

    "forever.asListOfDates() is a lazy view of 7 304 850 dates, cheap to create and to read at its ends" {
        val dates = MpLocalDateRange.forever.asListOfDates()

        (dates is ComputedList<*>) shouldBe true
        (dates is RandomAccess) shouldBe true
        dates.size shouldBe 7_304_850
        dates.first() shouldBe MpLocalDate.of(-10000, 1, 1)
        dates.last() shouldBe MpLocalDate.of(9999, 12, 31)
        dates[1_000_000] shouldBe MpLocalDate.of(-7263, 11, 28) // independent day count
    }

    "MpClosedLocalDateRange.forever: the same kind of view; the clamp's half-day rounding counts Doomsday itself" {
        val dates = MpClosedLocalDateRange.forever.asListOfDates()

        (dates is ComputedList<*>) shouldBe true
        dates.size shouldBe 7_304_851
        dates.last() shouldBe MpLocalDate.of(10000, 1, 1)
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
