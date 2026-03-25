package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MpTemporalPeriodSpec : StringSpec({

    "plus adds all components" {
        val a = MpDatePeriod(years = 1, months = 2, days = 3)
        val b = MpDateTimePeriod(years = 0, months = 1, days = 0, hours = 4, minutes = 5)

        val result = a + b

        result.years shouldBe 1
        result.months shouldBe 3
        result.days shouldBe 3
        result.hours shouldBe 4
        result.minutes shouldBe 5
    }

    "minus subtracts all components" {
        val a = MpDateTimePeriod(years = 2, months = 6, days = 10, hours = 8)
        val b = MpDateTimePeriod(years = 1, months = 3, days = 5, hours = 2)

        val result = a - b

        result.years shouldBe 1
        result.months shouldBe 3
        result.days shouldBe 5
        result.hours shouldBe 6
    }

    "plus two MpDatePeriods" {
        val a = MpDatePeriod(years = 1, months = 0, days = 10)
        val b = MpDatePeriod(years = 0, months = 6, days = 20)

        val result = a + b

        result.years shouldBe 1
        result.months shouldBe 6
        result.days shouldBe 30
        result.hours shouldBe 0
    }
})
