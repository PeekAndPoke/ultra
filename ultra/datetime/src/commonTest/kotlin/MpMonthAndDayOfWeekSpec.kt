package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** ULTRA-00210: the Mp month and day-of-week types, their factories, converters and serialization. */
@Suppress("unused")
class MpMonthAndDayOfWeekSpec : StringSpec({

    "MpMonth: numbers 1 to 12 in order, the same names and order as kotlinx" {
        MpMonth.entries.map { it.number } shouldBe (1..12).toList()
        MpMonth.entries.map { it.name } shouldBe Month.entries.map { it.name }
    }

    "MpDayOfWeek: ISO numbers 1 to 7 from Monday, the same names and order as kotlinx" {
        MpDayOfWeek.entries.map { it.isoDayNumber } shouldBe (1..7).toList()
        MpDayOfWeek.entries.map { it.name } shouldBe DayOfWeek.entries.map { it.name }
    }

    "MpMonth.of / ofOrNull / ofOrDefault for every valid number" {
        (1..12).forEach { n ->
            MpMonth.of(n).number shouldBe n
            MpMonth.ofOrNull(n)?.number shouldBe n
            MpMonth.ofOrDefault(n, MpMonth.JANUARY).number shouldBe n
        }
    }

    listOf(0, 13, -1, Int.MIN_VALUE, Int.MAX_VALUE).forEach { n ->
        "MpMonth.of($n) throws, ofOrNull is null, ofOrDefault gives the default" {
            shouldThrow<IllegalArgumentException> { MpMonth.of(n) }.message shouldBe "Invalid month number: $n"
            MpMonth.ofOrNull(n) shouldBe null
            MpMonth.ofOrDefault(n, MpMonth.MAY) shouldBe MpMonth.MAY
        }
    }

    "MpDayOfWeek.of / ofOrNull / ofOrDefault for every valid number" {
        (1..7).forEach { n ->
            MpDayOfWeek.of(n).isoDayNumber shouldBe n
            MpDayOfWeek.ofOrNull(n)?.isoDayNumber shouldBe n
            MpDayOfWeek.ofOrDefault(n, MpDayOfWeek.MONDAY).isoDayNumber shouldBe n
        }
    }

    listOf(0, 8, -1, Int.MIN_VALUE, Int.MAX_VALUE).forEach { n ->
        "MpDayOfWeek.of($n) throws, ofOrNull is null, ofOrDefault gives the default" {
            shouldThrow<IllegalArgumentException> { MpDayOfWeek.of(n) }.message shouldBe "Invalid ISO day number: $n"
            MpDayOfWeek.ofOrNull(n) shouldBe null
            MpDayOfWeek.ofOrDefault(n, MpDayOfWeek.SUNDAY) shouldBe MpDayOfWeek.SUNDAY
        }
    }

    "kotlinx converters, both ways, for every entry" {
        Month.entries.forEach { month ->
            month.mp.name shouldBe month.name
            month.mp.kotlinx shouldBe month
        }
        DayOfWeek.entries.forEach { day ->
            day.mp.name shouldBe day.name
            day.mp.kotlinx shouldBe day
        }
    }

    "month and dayOfWeek agree with the kotlinx values for every day of 2024 and 2025" {
        var date = MpLocalDate.of(2024, 1, 1)

        while (date.year < 2026) {
            date.month shouldBe date.kotlinx.month.mp
            date.dayOfWeek shouldBe date.kotlinx.dayOfWeek.mp

            date = date.plusDays(1)
        }
    }

    "the Mp inputs give the same results as the kotlinx inputs" {
        val date = MpLocalDate.of(2025, 3, 12) // a Wednesday
        val zoned = date.atStartOfDay(MpTimezone.of("Europe/Berlin"))

        MpLocalDate.of(2025, MpMonth.MARCH, 12) shouldBe MpLocalDate.of(2025, Month.MARCH, 12)
        MpLocalDateTime.of(2025, MpMonth.MARCH, 12, 1, 2, 3) shouldBe MpLocalDateTime.of(2025, Month.MARCH, 12, 1, 2, 3)

        date.atStartOfNext(MpDayOfWeek.FRIDAY) shouldBe date.atStartOfNext(DayOfWeek.FRIDAY)
        date.atStartOfNext(MpDayOfWeek.FRIDAY) shouldBe MpLocalDate.of(2025, 3, 14)
        date.atStartOfPrevious(MpDayOfWeek.MONDAY) shouldBe MpLocalDate.of(2025, 3, 10)
        date.getDatesInMonth(listOf(MpDayOfWeek.MONDAY)) shouldBe date.getDatesInMonth(listOf(DayOfWeek.MONDAY))
        date.getDatesInMonth(listOf(MpDayOfWeek.MONDAY)).map { it.day } shouldBe listOf(3, 10, 17, 24, 31)

        zoned.atStartOfNext(MpDayOfWeek.FRIDAY) shouldBe zoned.atStartOfNext(DayOfWeek.FRIDAY)
        zoned.atStartOfPrevious(MpDayOfWeek.MONDAY) shouldBe zoned.atStartOfPrevious(DayOfWeek.MONDAY)
    }

    "both types serialize by name as fields (kotlinx Json)" {
        val value = Holder(month = MpMonth.APRIL, dayOfWeek = MpDayOfWeek.TUESDAY)

        val json = Json.encodeToString(Holder.serializer(), value)

        json shouldBe """{"month":"APRIL","dayOfWeek":"TUESDAY"}"""
        Json.decodeFromString(Holder.serializer(), json) shouldBe value
    }
})

@Serializable
private data class Holder(val month: MpMonth, val dayOfWeek: MpDayOfWeek)
