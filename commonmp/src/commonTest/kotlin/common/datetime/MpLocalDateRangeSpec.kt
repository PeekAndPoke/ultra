package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.model.Operators
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

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

    "Comparing must work with Operators.Comparison" {

        fun range(days: Int) = MpLocalDate.parse("2022-06-15").toRange(MpDatePeriod(days = days))

        val twoDaysPeriod = MpDatePeriod(days = 2)

        Operators.Comparison.LT(range(1), twoDaysPeriod) shouldBe true
        Operators.Comparison.LT(range(2), twoDaysPeriod) shouldBe false
        Operators.Comparison.LT(range(3), twoDaysPeriod) shouldBe false

        Operators.Comparison.LTE(range(1), twoDaysPeriod) shouldBe true
        Operators.Comparison.LTE(range(2), twoDaysPeriod) shouldBe true
        Operators.Comparison.LTE(range(3), twoDaysPeriod) shouldBe false

        Operators.Comparison.EQ(range(1), twoDaysPeriod) shouldBe false
        Operators.Comparison.EQ(range(2), twoDaysPeriod) shouldBe true
        Operators.Comparison.EQ(range(3), twoDaysPeriod) shouldBe false

        Operators.Comparison.GTE(range(1), twoDaysPeriod) shouldBe false
        Operators.Comparison.GTE(range(2), twoDaysPeriod) shouldBe true
        Operators.Comparison.GTE(range(3), twoDaysPeriod) shouldBe true

        Operators.Comparison.GT(range(1), twoDaysPeriod) shouldBe false
        Operators.Comparison.GT(range(2), twoDaysPeriod) shouldBe false
        Operators.Comparison.GT(range(3), twoDaysPeriod) shouldBe true
    }
})
