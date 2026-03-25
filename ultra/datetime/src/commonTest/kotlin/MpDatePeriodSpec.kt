package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MpDatePeriodSpec : StringSpec({

    "Zero constant has all zero components" {
        MpDatePeriod.Zero shouldBe MpDatePeriod(years = 0, months = 0, days = 0)
    }

    "parse full period" {
        val result = MpDatePeriod.parse("P1Y2M3D")

        result.years shouldBe 1
        result.months shouldBe 2
        result.days shouldBe 3
    }

    "parse years only" {
        val result = MpDatePeriod.parse("P2Y")

        result.years shouldBe 2
        result.months shouldBe 0
        result.days shouldBe 0
    }

    "parse days only" {
        val result = MpDatePeriod.parse("P30D")

        result.years shouldBe 0
        result.months shouldBe 0
        result.days shouldBe 30
    }

    "unaryMinus negates all components" {
        val period = MpDatePeriod(years = 1, months = 2, days = 3)
        val negated = -period

        negated.years shouldBe -1
        negated.months shouldBe -2
        negated.days shouldBe -3
    }

    "time components are always zero" {
        val period = MpDatePeriod(years = 1, months = 2, days = 3)

        period.hours shouldBe 0
        period.minutes shouldBe 0
        period.seconds shouldBe 0
        period.milliseconds shouldBe 0
    }
})
