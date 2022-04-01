package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.fail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month

@Suppress("unused")
class MpLocalDateTimeSpec : StringSpec({

    "Construction" {

        MpLocalDateTime.of(year = 2022, month = 4, day = 1) shouldBe
                MpLocalDateTime.parse("2022-04-01T00:00:00.000")

        MpLocalDateTime.of(year = 2022, month = Month.APRIL, day = 1) shouldBe
                MpLocalDateTime.parse("2022-04-01T00:00:00.000")

    }

    "Genesis and Doomsday" {

        MpLocalDateTime.Genesis shouldBe MpLocalDateTime.of(-10000, Month.JANUARY, 1)

        MpLocalDateTime.Genesis.toString() shouldBe "-10000-01-01T00:00"

        MpLocalDateTime.Doomsday shouldBe MpLocalDateTime.of(10000, Month.JANUARY, 1)

        MpLocalDateTime.Doomsday.toString() shouldBe "+10000-01-01T00:00"
    }

    "Equality" {

        MpLocalDateTime.parse("2022-04-01T12:00:00.000") shouldBeEqualComparingTo
                MpLocalDateTime.parse("2022-04-01T12:00:00.000")

        MpLocalDateTime.parse("2022-04-01T12:00:00.000") shouldBeLessThan
                MpLocalDateTime.parse("2022-04-02T12:00:00.000")

        MpLocalDateTime.parse("2022-04-02T12:00:00.000") shouldBeGreaterThan
                MpLocalDateTime.parse("2022-04-01T12:00:00.000")
    }

    "Fields year, monthNumber, month, dayOfMonth, dayOfWeek, dayOfYear, hour, minute, second, nano" {

        val subject = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14, 15)

        subject.year shouldBe 2022
        subject.monthNumber shouldBe 4
        subject.month shouldBe Month.APRIL
        subject.dayOfMonth shouldBe 5
        subject.dayOfWeek shouldBe DayOfWeek.TUESDAY
        subject.dayOfYear shouldBe 95

        subject.hour shouldBe 12
        subject.minute shouldBe 13
        subject.second shouldBe 14
        subject.nanosecond shouldBe 15
    }

    "toInstant" {
        fail("check me")
    }

    "atZone" {
        fail("check me")
    }
})
