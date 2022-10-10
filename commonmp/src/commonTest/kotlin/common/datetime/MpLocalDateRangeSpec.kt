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

    "toRange(period: MpDatePeriod)" {
        MpLocalDate.parse("2022-03-04").let { from ->
            val period = MpDatePeriod(years = 1, months = 2, days = 3)

            from.toRange(period) shouldBe MpLocalDateRange(from, from.plus(period))
        }
    }

    "asDatePeriod for invalid MpLocalDateRange" {

        val invalid = MpLocalDateRange(
            from = MpLocalDate.parse("2021-01-01"),
            to = MpLocalDate.parse("2020-01-01"),
        )

        invalid.asDatePeriod shouldBe MpDatePeriod.Zero
    }

    "asDatePeriod for valid MpLocalDateRanges" {

        val start = MpLocalDate.parse("2020-01-01")

        val inputs = listOf(
            MpDatePeriod(years = 0, months = 0, days = 0),
            MpDatePeriod(years = 0, months = 0, days = 1),
            MpDatePeriod(years = 0, months = 1, days = 1),
            MpDatePeriod(years = 1, months = 1, days = 1),
            MpDatePeriod(years = 2, months = 2, days = 2),
            MpDatePeriod(years = 20, months = 10, days = 20),
        )

        inputs.forEach { input ->
            val range = start.toRange(input)

            withClue("${range.from.formatDdMmmYyyy()}-${range.to.formatDdMmmYyyy()}  must have period $input") {
                range.asDatePeriod shouldBe input
            }
        }
    }
})
