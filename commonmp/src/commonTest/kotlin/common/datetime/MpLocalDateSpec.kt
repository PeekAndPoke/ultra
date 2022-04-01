package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt

@Suppress("unused")
class MpLocalDateSpec : StringSpec({

    "Construction" {

        MpLocalDate.of(year = 2022, month = 4, day = 1) shouldBe
                MpLocalDate.parse("2022-04-01")

        MpLocalDate.of(year = 2022, month = Month.APRIL, day = 1) shouldBe
                MpLocalDate.parse("2022-04-01")

    }

    "Genesis and Doomsday" {

        MpLocalDate.Genesis shouldBe MpLocalDate.of(-10000, Month.JANUARY, 1)

        MpLocalDate.Genesis.toString() shouldBe "-10000-01-01"

        MpLocalDate.Doomsday shouldBe MpLocalDate.of(10000, Month.JANUARY, 1)

        MpLocalDate.Doomsday.toString() shouldBe "+10000-01-01"
    }

    "Equality" {

        MpLocalDate.parse("2022-04-01") shouldBeEqualComparingTo
                MpLocalDate.parse("2022-04-01")

        MpLocalDate.parse("2022-04-01") shouldBeLessThan
                MpLocalDate.parse("2022-04-02")

        MpLocalDate.parse("2022-04-02") shouldBeGreaterThan
                MpLocalDate.parse("2022-04-01")
    }

    "Fields year, monthNumber, month, dayOfMonth, dayOfWeek, dayOfYear" {

        val subject = MpLocalDate.of(2022, Month.APRIL, 5)

        subject.year shouldBe 2022
        subject.monthNumber shouldBe 4
        subject.month shouldBe Month.APRIL
        subject.dayOfMonth shouldBe 5
        subject.dayOfWeek shouldBe DayOfWeek.TUESDAY
        subject.dayOfYear shouldBe 95
    }

    "Arithmetic - plus days" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.plus(DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 6)
        start.plus(2, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 7)
        start.plus(2L, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 7)
        start.plus(-2, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 3)
        start.plus(-2L, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 3)
    }

    "Arithmetic - minus days" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.minus(DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 4)
        start.minus(2, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 3)
        start.minus(2L, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 3)
        start.minus(-2, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 7)
        start.minus(-2L, DateTimeUnit.DAY) shouldBe MpLocalDate.of(2022, Month.APRIL, 7)
    }

    "Arithmetic - plus months" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.plus(DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.MAY, 5)
        start.plus(5, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.SEPTEMBER, 5)
        start.plus(5L, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.SEPTEMBER, 5)
        start.plus(-5, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2021, Month.NOVEMBER, 5)
        start.plus(-5L, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2021, Month.NOVEMBER, 5)
    }

    "Arithmetic - minus months" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.minus(DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.MARCH, 5)
        start.minus(5, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2021, Month.NOVEMBER, 5)
        start.minus(5L, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2021, Month.NOVEMBER, 5)
        start.minus(-5, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.SEPTEMBER, 5)
        start.minus(-5L, DateTimeUnit.MONTH) shouldBe MpLocalDate.of(2022, Month.SEPTEMBER, 5)
    }

    "Arithmetic - plus years" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.plus(DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2023, Month.APRIL, 5)
        start.plus(5, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2027, Month.APRIL, 5)
        start.plus(5L, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2027, Month.APRIL, 5)
        start.plus(-5, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2017, Month.APRIL, 5)
        start.plus(-5L, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2017, Month.APRIL, 5)
    }

    "Arithmetic - minus years" {

        val start = MpLocalDate.of(2022, Month.APRIL, 5)

        start.minus(DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2021, Month.APRIL, 5)
        start.minus(5, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2017, Month.APRIL, 5)
        start.minus(5L, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2017, Month.APRIL, 5)
        start.minus(-5, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2027, Month.APRIL, 5)
        start.minus(-5L, DateTimeUnit.YEAR) shouldBe MpLocalDate.of(2027, Month.APRIL, 5)
    }

    "atStartOfDay - Bucharest" {

        val timezone = TimeZone.of("Europe/Bucharest")

        val date = MpLocalDate.of(2022, Month.APRIL, 5)

        val startOfDay = date.atStartOfDay(timezone)

        withClue("Bucharest is 3 hours ahead of UTC") {
            startOfDay.toString() shouldBe "2022-04-04T21:00:00Z"
        }
    }


    TestConstants.timeZoneIds.forEach { timezoneId ->

        "atStartOfDay - at timezone '$timezoneId'" {

            val date = MpLocalDate.of(2022, Month.APRIL, 5)
            val instant = date.atStartOfDay(TimeZone.UTC)

            val timezone = TimeZone.of(timezoneId)

            val startOfDay = date.atStartOfDay(timezone)

            val startOfDayTs = startOfDay.toEpochSeconds()

            val offset = timezone.offsetAt(instant.value).totalSeconds
            val expectedTs = instant.toEpochSeconds() - offset

            startOfDayTs shouldBe expectedTs
        }
    }
})
