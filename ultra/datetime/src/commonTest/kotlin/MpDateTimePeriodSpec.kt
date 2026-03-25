package de.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MpDateTimePeriodSpec : StringSpec({

    "Zero constant has all zero components" {
        val zero = MpDateTimePeriod.Zero

        zero.years shouldBe 0
        zero.months shouldBe 0
        zero.days shouldBe 0
        zero.hours shouldBe 0
        zero.minutes shouldBe 0
        zero.seconds shouldBe 0
        zero.milliseconds shouldBe 0
    }

    "parse period string" {
        val result = MpDateTimePeriod.parse("P1Y6M")

        result.years shouldBe 1
        result.months shouldBe 6
        result.days shouldBe 0
    }

    "of(Duration) converts correctly" {
        val result = MpDateTimePeriod.of(2.hours + 30.minutes)

        result.hours shouldBe 2
        result.minutes shouldBe 30
        result.years shouldBe 0
    }

    "unaryMinus negates all components" {
        val period =
            MpDateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5, seconds = 6, milliseconds = 7)
        val negated = -period

        negated.years shouldBe -1
        negated.months shouldBe -2
        negated.days shouldBe -3
        negated.hours shouldBe -4
        negated.minutes shouldBe -5
        negated.seconds shouldBe -6
        negated.milliseconds shouldBe -7
    }

    "toDatePeriod strips time components" {
        val period = MpDateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5)
        val datePeriod = period.toDatePeriod()

        datePeriod.years shouldBe 1
        datePeriod.months shouldBe 2
        datePeriod.days shouldBe 3
        datePeriod.hours shouldBe 0
        datePeriod.minutes shouldBe 0
    }
})
