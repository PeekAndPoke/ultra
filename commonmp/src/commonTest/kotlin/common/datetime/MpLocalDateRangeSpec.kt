package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class MpLocalDateRangeSpec : StringSpec({

    "compareTo(period: MpTemporalPeriod) - 1 day" {

        val subject = MpLocalDate.parse("2022-06-15").toRange(MpDatePeriod(days = 1))

        withClue("Compared with empty period") {
            val period = MpDatePeriod.Zero

            (subject isGreaterThan period).shouldBeTrue()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeFalse()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 1 day period") {
            val period = MpDatePeriod(days = 1)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 2 days") {
            val period = MpDatePeriod(days = 2)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeFalse()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeTrue()
        }
    }
})
