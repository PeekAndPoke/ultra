package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.model.Operators
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MpClosedLocalDateRangeSpec : StringSpec({

    "asOpenRange" {
        val subject: MpClosedLocalDateRange =
            MpLocalDate.parse("2022-06-15").toClosedRange(MpDatePeriod(days = 10))

        subject.asOpenRange.from shouldBe subject.from
        subject.asOpenRange.to shouldBe subject.to.plusDays(1)
    }

    "compareTo(period: MpTemporalPeriod) - 1 day" {

        val subject: MpClosedLocalDateRange =
            MpLocalDate.parse("2022-06-15").toClosedRange(MpDatePeriod(days = 2))

        withClue("The number of whole days must be correct") {
            subject.numberOfDays shouldBe 2
        }

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

            (subject isGreaterThan period).shouldBeTrue()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeFalse()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 2 days") {
            val period = MpDatePeriod(days = 2)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }
    }

    "Comparing must work with Operators.Comparison" {

        fun range(days: Int) = MpLocalDate.parse("2022-06-15").toClosedRange(MpDatePeriod(days = days))

        val twoDaysPeriod = MpDatePeriod(days = 1)

        Operators.Comparison.LT(range(0), twoDaysPeriod) shouldBe true
        Operators.Comparison.LT(range(1), twoDaysPeriod) shouldBe false
        Operators.Comparison.LT(range(2), twoDaysPeriod) shouldBe false

        Operators.Comparison.LTE(range(0), twoDaysPeriod) shouldBe true
        Operators.Comparison.LTE(range(1), twoDaysPeriod) shouldBe true
        Operators.Comparison.LTE(range(2), twoDaysPeriod) shouldBe false

        Operators.Comparison.EQ(range(0), twoDaysPeriod) shouldBe false
        Operators.Comparison.EQ(range(1), twoDaysPeriod) shouldBe true
        Operators.Comparison.EQ(range(2), twoDaysPeriod) shouldBe false

        Operators.Comparison.GTE(range(0), twoDaysPeriod) shouldBe false
        Operators.Comparison.GTE(range(1), twoDaysPeriod) shouldBe true
        Operators.Comparison.GTE(range(2), twoDaysPeriod) shouldBe true

        Operators.Comparison.GT(range(0), twoDaysPeriod) shouldBe false
        Operators.Comparison.GT(range(1), twoDaysPeriod) shouldBe false
        Operators.Comparison.GT(range(2), twoDaysPeriod) shouldBe true
    }

    "toClosedRange(period: MpDatePeriod)" {
        MpLocalDate.parse("2022-03-04").let { from ->
            val period = MpDatePeriod(years = 1, months = 2, days = 3)

            from.toClosedRange(period) shouldBe MpClosedLocalDateRange(from, from.plus(period).minusDays(1))
        }
    }

    "asDatePeriod for invalid range" {

        val invalid = MpClosedLocalDateRange(
            from = MpLocalDate.parse("2021-01-02"),
            to = MpLocalDate.parse("2020-01-01"),
        )

        invalid.numberOfDays shouldBe 0
        invalid.asDatePeriod shouldBe MpDatePeriod(days = 0)
    }

    "asDatePeriod for single day" {

        val singleDay = MpClosedLocalDateRange(
            from = MpLocalDate.parse("2021-01-01"),
            to = MpLocalDate.parse("2021-01-01"),
        )

        singleDay.numberOfDays shouldBe 1
        singleDay.asDatePeriod shouldBe MpDatePeriod(days = 1)
    }

    "asDatePeriod for valid ranges" {

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
            val range = start.toClosedRange(input)

            withClue("${range.from.formatDdMmmYyyy()}-${range.to.formatDdMmmYyyy()} must have period $input") {
                range.asDatePeriod shouldBe input
            }
        }
    }

    "numberOfDays" {

        val closedRangeExtraDay = 1

        withClue("for invalid range") {
            MpLocalDate.parse("2022-01-02")
                .toClosedRange(MpLocalDate.parse("2022-01-01"))
                .numberOfDays shouldBe 0
        }

        withClue("for same day") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2022-03-27"))
                .numberOfDays shouldBe closedRangeExtraDay
        }

        withClue("for DST switch - one day") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2022-03-28"))
                .numberOfDays shouldBe 1 + closedRangeExtraDay
        }

        withClue("for DST switch - one year") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2023-03-27"))
                .numberOfDays shouldBe 365 + closedRangeExtraDay
        }

        withClue("with leap year") {
            MpLocalDate.parse("2020-01-01")
                .toClosedRange(MpLocalDate.parse("2021-01-01"))
                .numberOfDays shouldBe (365 + 1 + closedRangeExtraDay)
        }

        withClue("no leap year - two years, three months, four days") {
            MpLocalDate.parse("2021-01-01")
                .toClosedRange(MpLocalDate.parse("2023-04-05"))
                .numberOfDays shouldBe (365 + 365 + 31 + 28 + 31 + 4 + closedRangeExtraDay)
        }
    }

    "numberOfNights" {

        val closedRangeExtraDay = 1

        withClue("for invalid range") {
            MpLocalDate.parse("2022-01-02")
                .toClosedRange(MpLocalDate.parse("2022-01-01"))
                .numberOfNights shouldBe 0
        }

        withClue("for same day") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2022-03-27"))
                .numberOfNights shouldBe 0
        }

        withClue("for DST switch - one day") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2022-03-28"))
                .numberOfNights shouldBe closedRangeExtraDay
        }

        withClue("for DST switch - one year") {
            MpLocalDate.parse("2022-03-27")
                .toClosedRange(MpLocalDate.parse("2023-03-27"))
                .numberOfNights shouldBe (365 - 1) + closedRangeExtraDay
        }

        withClue("with leap year") {
            MpLocalDate.parse("2020-01-01")
                .toClosedRange(MpLocalDate.parse("2021-01-01"))
                .numberOfNights shouldBe (365 + closedRangeExtraDay)
        }

        withClue("no leap year - two years, three months, four days") {
            MpLocalDate.parse("2021-01-01")
                .toClosedRange(MpLocalDate.parse("2023-04-05"))
                .numberOfNights shouldBe (365 + 365 + 31 + 28 + 31 + 3 + closedRangeExtraDay)
        }
    }

    "toZonedTimeRange().fromMorningToEvening" {

        val range = MpLocalDate.parse("2020-01-01").toClosedRange(MpLocalDate.parse("2021-01-02"))
        val timezone = MpTimezone.of("Europe/Berlin")

        val zoned = range.toZonedTimeRange(timezone).fromMorningToEvening

        zoned.from shouldBe range.from.atStartOfDay(timezone)
        zoned.to shouldBe range.to.atStartOfDay(timezone).plus(1.days)
    }

    "toZonedTimeRange().fromHourToHour" {

        val range = MpLocalDate.parse("2020-01-01").toClosedRange(MpLocalDate.parse("2021-01-02"))
        val timezone = MpTimezone.of("Europe/Berlin")

        val zoned = range.toZonedTimeRange(timezone).fromHourToHour(
            fromHour = 15,
            toHour = 11,
        )

        zoned.from shouldBe range.from.atStartOfDay(timezone).plus(15.hours)
        zoned.to shouldBe range.to.atStartOfDay(timezone).plus(11.hours)
    }

    "toZonedTimeRange().fromTimeToTime" {

        val range = MpLocalDate.parse("2020-01-01").toClosedRange(MpLocalDate.parse("2021-01-02"))
        val timezone = MpTimezone.of("Europe/Berlin")

        val zoned = range.toZonedTimeRange(timezone).fromTimeToTime(
            fromTime = MpLocalTime.of(hour = 1, minute = 2, second = 3, milliSecond = 4),
            toTime = MpLocalTime.of(hour = 21, minute = 22, second = 23, milliSecond = 24),
        )

        zoned.from shouldBe range.from.atStartOfDay(timezone)
            .plus(1.hours).plus(2.minutes).plus(3.seconds).plus(4.milliseconds)

        zoned.to shouldBe range.to.atStartOfDay(timezone)
            .plus(21.hours).plus(22.minutes).plus(23.seconds).plus(24.milliseconds)
    }

    "asListOfDates()" {

        withClue("Invalid range must return an empty list") {
            val start = MpLocalDate.parse("2020-01-02")
            val range = start.toClosedRange(start.minusDays(1))
            val list = range.asListOfDates()
            list.shouldBeEmpty()
            list shouldHaveSize range.numberOfDays
        }

        withClue("Range of a single day must return 1 date") {
            val start = MpLocalDate.parse("2020-01-02")
            val range = start.toClosedRange(start)
            val list = range.asListOfDates()
            list shouldHaveSize 1
            list shouldHaveSize range.numberOfDays
            list shouldBeEqual listOf(
                start,
            )
        }

        withClue("Range of more days must return the correct dates") {
            val start = MpLocalDate.parse("2020-01-02")
            val range = start.toClosedRange(start.plusDays(2))
            val list = range.asListOfDates()
            list shouldHaveSize 3
            list shouldHaveSize range.numberOfDays
            list shouldBeEqual listOf(
                start,
                start.plusDays(1),
                start.plusDays(2),
            )
        }
    }
})
