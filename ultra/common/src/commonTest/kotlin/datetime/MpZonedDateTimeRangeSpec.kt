package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.model.Operators
import de.peekandpoke.ultra.common.model.tuple
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class MpZonedDateTimeRangeSpec : StringSpec({

    val now = MpInstant.now().atSystemDefaultZone()

    "forever - range from Genesis to Doomsday" {
        MpZonedDateTimeRange.forever.from shouldBe MpZonedDateTime.Genesis
        MpZonedDateTimeRange.forever.to shouldBe MpZonedDateTime.Doomsday
        MpZonedDateTimeRange.forever.isValid.shouldBeTrue()
    }

    "of(from, duration) - creates range from 'from' with duration" {
        val duration = 3.hours
        val result = MpZonedDateTimeRange.of(from = now, duration)
        result.from shouldBe now
        result.to shouldBe now.plus(duration)
        result.isValid.shouldBeTrue()
    }

    "of(from, period) - creates range from 'from' with period" {
        val period = MpDatePeriod(days = 1)
        val result = MpZonedDateTimeRange.of(from = now, period)
        result.from shouldBe now
        result.to shouldBe now.plus(period)
        result.isValid.shouldBeTrue()
    }

    "beginningAt(from) - creates range from 'from' to Doomsday" {
        val result = MpZonedDateTimeRange.beginningAt(from = now)
        result.from shouldBe now
        result.to shouldBe MpZonedDateTime.Doomsday
        result.isValid.shouldBeTrue()
    }

    "endingAt(to) - creates range from Genesis to 'to'" {
        val result = MpZonedDateTimeRange.endingAt(to = now)
        result.from shouldBe MpZonedDateTime.Genesis
        result.to shouldBe now
        result.isValid.shouldBeTrue()
    }

    "duration - returns the correct duration of the range" {
        val range = now.toRange(3.hours)
        range.duration shouldBe 3.hours
    }

    "hasStart - returns true if range has a valid start" {
        val range = MpZonedDateTimeRange(now, MpZonedDateTime.Doomsday)
        range.hasStart.shouldBeTrue()

        val openStart = MpZonedDateTimeRange(MpZonedDateTime.Genesis, now)
        openStart.hasStart.shouldBeFalse()
    }

    "hasEnd - returns true if range has a valid end" {
        val range = MpZonedDateTimeRange(MpZonedDateTime.Genesis, now)
        range.hasEnd.shouldBeTrue()

        val openEnd = MpZonedDateTimeRange(now, MpZonedDateTime.Doomsday)
        openEnd.hasEnd.shouldBeFalse()
    }

    "isOpen - returns true if range is open-ended" {
        val range = MpZonedDateTimeRange(MpZonedDateTime.Genesis, MpZonedDateTime.Doomsday)
        range.isOpen.shouldBeTrue()

        val closedRange = MpZonedDateTimeRange(now, now.plus(1.hours))
        closedRange.isOpen.shouldBeFalse()
    }

    "isNotOpen - returns true if range is not open-ended" {
        val closedRange = MpZonedDateTimeRange(now, now.plus(1.hours))
        closedRange.isNotOpen.shouldBeTrue()

        val openRange = MpZonedDateTimeRange(MpZonedDateTime.Genesis, MpZonedDateTime.Doomsday)
        openRange.isNotOpen.shouldBeFalse()
    }

    "isValid - returns false for invalid ranges" {
        val validRange = MpZonedDateTimeRange(now, now.plus(3.hours))
        validRange.isValid.shouldBeTrue()

        val invalidRange = MpZonedDateTimeRange(now.plus(3.hours), now)
        invalidRange.isValid.shouldBeFalse()
    }

    "Partial.asValidRange() - converts a partial range to valid full range" {
        val partial = MpZonedDateTimeRange.Partial(from = now, to = now.plus(1.hours))
        val result = partial.asValidRange()

        result?.from shouldBe now
        result?.to shouldBe now.plus(1.hours)
        result?.isValid?.shouldBeTrue()
    }

    "Partial.asDateRange() - converts to MpClosedLocalDateRange.Partial" {
        val partial = MpZonedDateTimeRange.Partial(from = now, to = now.plus(1.hours))
        val dateRange = partial.asDateRange()

        dateRange.from shouldBe now.toLocalDate()
        dateRange.to shouldBe now.plus(1.hours).toLocalDate()
    }

    "plus(duration) - adds duration to range" {
        val range = now.toRange(3.hours)

        val result = range.plus(1.hours)
        result.from shouldBe range.from.plus(1.hours)
        result.to shouldBe range.to.plus(1.hours)
    }

    "plus(unit) - adds DateTimeUnit to range" {
        val range = now.toRange(3.hours)

        val result = range.plus(DateTimeUnit.HOUR)
        result.from shouldBe range.from.plus(1, DateTimeUnit.HOUR)
        result.to shouldBe range.to.plus(1, DateTimeUnit.HOUR)
    }

    "plus(value, unit) - adds specific value of DateTimeUnit" {
        val range = now.toRange(3.hours)

        val result = range.plus(2, DateTimeUnit.DAY)
        result.from shouldBe range.from.plus(2, DateTimeUnit.DAY)
        result.to shouldBe range.to.plus(2, DateTimeUnit.DAY)
    }

    "minus(duration) - subtracts duration from range" {
        val range = now.toRange(3.hours)

        val result = range.minus(1.hours)
        result.from shouldBe range.from.minus(1.hours)
        result.to shouldBe range.to.minus(1.hours)
    }

    "minus(unit) - subtracts DateTimeUnit from range" {
        val range = now.toRange(3.hours)

        val result = range.minus(DateTimeUnit.HOUR)
        result.from shouldBe range.from.minus(1, DateTimeUnit.HOUR)
        result.to shouldBe range.to.minus(1, DateTimeUnit.HOUR)
    }

    "minus(value, unit) - subtracts specific value of DateTimeUnit" {
        val range = now.toRange(3.hours)

        val result = range.minus(2, DateTimeUnit.DAY)
        result.from shouldBe range.from.minus(2, DateTimeUnit.DAY)
        result.to shouldBe range.to.minus(2, DateTimeUnit.DAY)
    }

    "formatDdMmmYyyyHhMm() - formats range as string" {
        val range = MpZonedDateTimeRange(
            from = MpZonedDateTime.parse("2025-05-22T17:45:00.000[UTC]"),
            to = MpZonedDateTime.parse("2025-05-22T20:45:00.000[UTC]"),
        )

        // Adjust if you want correct alignment with current formatting logic
        val expected = "22 May 2025 17:45 - 20:45"

        range.formatDdMmmYyyyHhMm() shouldBe expected
    }

    "compareTo(period: MpTemporalPeriod) - 1 day in UTC" {

        val subject = Kronos.systemUtc.zonedDateTimeNow(MpTimezone.UTC).toRange(MpDatePeriod(days = 1))

        withClue("Compared with empty period") {
            val period = MpDateTimePeriod.Zero

            (subject isGreaterThan period).shouldBeTrue()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeFalse()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 1 day period") {
            val period = MpDateTimePeriod(days = 1)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 24 hours period") {
            val period = MpDateTimePeriod(hours = 24)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 1 day plus 1 milli") {
            val period = MpDateTimePeriod(days = 1) + MpDateTimePeriod(milliseconds = 1)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeFalse()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeTrue()
        }

        withClue("Compared with 1 day minus 1 milli") {
            val period = MpDateTimePeriod(days = 1) - MpDateTimePeriod(milliseconds = 1)

            (subject isGreaterThan period).shouldBeTrue()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeFalse()
            (subject isLessThan period).shouldBeFalse()
        }
    }

    "compareTo(period: MpTemporalPeriod) - 1 day in Europe/Berlin at DST switch" {

        val subject = MpZonedDateTimeRange(
            from = MpZonedDateTime.parse("2022-03-27T00:00:00.000[Europe/Berlin]"),
            to = MpZonedDateTime.parse("2022-03-28T00:00:00.000[Europe/Berlin]"),
        )

        withClue("Compared with empty period") {
            val period = MpDateTimePeriod.Zero

            (subject isGreaterThan period).shouldBeTrue()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeFalse()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 1 day period") {
            val period = MpDateTimePeriod(days = 1)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 23 hours period") {
            val period = MpDateTimePeriod(hours = 23)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeTrue()
            (subject isEqualTo period).shouldBeTrue()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeFalse()
        }

        withClue("Compared with 23 hours and 1 milli") {
            val period = MpDateTimePeriod(hours = 23) + MpDateTimePeriod(milliseconds = 1)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeFalse()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeTrue()
        }

        withClue("Compared with 24 hours period") {
            val period = MpDateTimePeriod(hours = 24)

            (subject isGreaterThan period).shouldBeFalse()
            (subject isGreaterThanOrEqualTo period).shouldBeFalse()
            (subject isEqualTo period).shouldBeFalse()
            (subject isLessThanOrEqualTo period).shouldBeTrue()
            (subject isLessThan period).shouldBeTrue()
        }
    }

    "Comparing must work with Operators.Comparison" {

        fun range(days: Int) = MpZonedDateTime.parse("2022-06-15T12:00:00").toRange(MpDatePeriod(days = days))

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

    listOf(
        tuple(
            "All eaten up - exactly",
            now.toRange(1.hours),
            now.toRange(1.hours),
            listOf(),
        ),
        tuple(
            "All eaten up - surrounding",
            now.toRange(1.hours),
            now.minus(1.seconds).toRange(1.hours + 2.seconds),
            listOf(),
        ),
        tuple(
            "In the middle",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(1.hours),
            listOf(
                now.toRange(1.hours),
                now.plus(2.hours).toRange(1.hours),
            ),
        ),
        tuple(
            "In the middle but duration = 0",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(0.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        tuple(
            "On the left side",
            now.toRange(3.hours),
            now.toRange(1.hours),
            listOf(
                now.plus(1.hours).toRange(2.hours)
            ),
        ),
        tuple(
            "On the right side",
            now.toRange(3.hours),
            now.plus(2.hours).toRange(1.hours),
            listOf(
                now.toRange(2.hours)
            ),
        ),
        tuple(
            "Too far on the left side",
            now.toRange(3.hours),
            now.minus(2.hours).toRange(2.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        tuple(
            "Too far on the right side",
            now.toRange(3.hours),
            now.plus(3.hours).toRange(2.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
    ).forEach { (title, source, other, expected) ->

        ("cutAway(other: MpZonedDateTimeRange) - " +
                "$title: ${source.format()} cut away ${other.format()} " +
                "must be ${expected.format()}  ") {

            source.cutAway(other) shouldBe expected
        }
    }

    "toInstantRange() - converts MpZonedDateTimeRange to MpInstantRange" {
        val range = now.toRange(3.hours)
        val instantRange = range.toInstantRange()

        instantRange.from shouldBe range.from.toInstant()
        instantRange.to shouldBe range.to.toInstant()
    }

    "asDateRange() - converts MpZonedDateTimeRange to MpClosedLocalDateRange" {
        val now = MpInstant.now().atSystemDefaultZone()
        val range = now.toRange(3.hours)
        val dateRange = range.asDateRange()

        dateRange.from shouldBe range.from.toLocalDate()
        dateRange.to shouldBe range.to.toLocalDate()
    }

    "atZone() - change timezone to UTC" {
        val range = now.toRange(3.hours)
        val utcZone = MpTimezone.of("UTC")
        val convertedRange = range.atZone(utcZone)

        convertedRange.from.timezone shouldBe utcZone
        convertedRange.to.timezone shouldBe utcZone
    }

    "atZone() - change timezone to Europe/Berlin" {
        val range = now.toRange(3.hours)
        val berlinZone = MpTimezone.of("Europe/Berlin")
        val convertedRange = range.atZone(berlinZone)

        convertedRange.from.timezone shouldBe berlinZone
        convertedRange.to.timezone shouldBe berlinZone
    }

    "atZone() - range remains valid after timezone conversion" {
        val range = now.toRange(3.hours)
        val someZone = TimeZone.of("America/New_York")
        val convertedRange = range.atZone(someZone)

        (convertedRange.from < convertedRange.to) shouldBe true
    }

    "contains() - datetime within range" {
        val range = now.toRange(3.hours)
        val datetime = now.plus(1.hours)

        range.contains(datetime).shouldBeTrue()
    }

    "contains() - datetime at start of range" {
        val range = now.toRange(3.hours)
        val datetime = now

        range.contains(datetime).shouldBeTrue()
    }

    "contains() - datetime at end of range" {
        val range = now.toRange(3.hours)
        val datetime = now.plus(3.hours)

        range.contains(datetime).shouldBeFalse() // End is exclusive
    }

    "contains() - datetime before range" {
        val range = now.toRange(3.hours)
        val datetime = now.minus(1.hours)

        range.contains(datetime).shouldBeFalse()
    }

    "contains() - datetime after range" {
        val range = now.toRange(3.hours)
        val datetime = now.plus(4.hours)

        range.contains(datetime).shouldBeFalse()
    }

    "contains() - one range fully contains another" {
        val outerRange = now.toRange(5.hours)
        val innerRange = now.plus(1.hours).toRange(2.hours)

        outerRange.contains(innerRange).shouldBeTrue()
    }

    "contains() - ranges are exact matches" {
        val range1 = now.toRange(3.hours)
        val range2 = now.toRange(3.hours)

        range1.contains(range2).shouldBeTrue()
    }

    "contains() - range partially overlaps" {
        val range1 = now.toRange(3.hours)
        val range2 = now.plus(2.hours).toRange(3.hours)

        range1.contains(range2).shouldBeFalse()
    }

    "contains() - range outside" {
        val range1 = now.toRange(3.hours)
        val range2 = now.plus(4.hours).toRange(2.hours)

        range1.contains(range2).shouldBeFalse()
    }

    "contains() - invalid inner range" {
        val outerRange = now.toRange(5.hours)
        val invalidRange = MpZonedDateTimeRange(now.plus(2.hours), now.plus(1.hours)) // Invalid range

        outerRange.contains(invalidRange).shouldBeFalse()
    }

    "contains() - invalid outer range" {
        val invalidRange = MpZonedDateTimeRange(now.plus(3.hours), now.plus(1.hours)) // Invalid range
        val innerRange = now.toRange(2.hours)

        invalidRange.contains(innerRange).shouldBeFalse()
    }

    "intersects() - overlapping ranges" {
        val range1 = now.toRange(3.hours)
        val range2 = now.plus(1.hours).toRange(3.hours)

        range1.intersects(range2).shouldBeTrue()
        range2.intersects(range1).shouldBeTrue()
    }

    "intersects() - fully contained range" {
        val range1 = now.toRange(5.hours)
        val range2 = now.plus(1.hours).toRange(2.hours)

        range1.intersects(range2).shouldBeTrue()
        range2.intersects(range1).shouldBeTrue()
    }

    "intersects() - exactly touching ranges" {
        val range1 = now.toRange(2.hours)
        val range2 = now.plus(2.hours).toRange(2.hours)

        range1.intersects(range2).shouldBeFalse()
        range2.intersects(range1).shouldBeFalse()
    }

    "intersects() - non-overlapping ranges (disjoint)" {
        val range1 = now.toRange(2.hours)
        val range2 = now.plus(3.hours).toRange(2.hours)

        range1.intersects(range2).shouldBeFalse()
        range2.intersects(range1).shouldBeFalse()
    }

    "intersects() - one range is invalid" {
        val range1 = now.toRange(2.hours)
        val range2 = MpZonedDateTimeRange(now.plus(3.hours), now.plus(2.hours)) // Invalid range

        range1.intersects(range2).shouldBeFalse()
        range2.intersects(range1).shouldBeFalse()
    }
})
