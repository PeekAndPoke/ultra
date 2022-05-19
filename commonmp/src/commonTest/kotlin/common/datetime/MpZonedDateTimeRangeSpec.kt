package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class MpZonedDateTimeRangeSpec : StringSpec({

    val now = MpInstant.now().atSystemDefaultZone()

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

    listOf(
        row(
            "All eaten up - exactly",
            now.toRange(1.hours),
            now.toRange(1.hours),
            listOf(),
        ),
        row(
            "All eaten up - surrounding",
            now.toRange(1.hours),
            now.minus(1.seconds).toRange(1.hours + 2.seconds),
            listOf(),
        ),
        row(
            "In the middle",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(1.hours),
            listOf(
                now.toRange(1.hours),
                now.plus(2.hours).toRange(1.hours),
            ),
        ),
        row(
            "In the middle but duration = 0",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(0.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        row(
            "On the left side",
            now.toRange(3.hours),
            now.toRange(1.hours),
            listOf(
                now.plus(1.hours).toRange(2.hours)
            ),
        ),
        row(
            "On the right side",
            now.toRange(3.hours),
            now.plus(2.hours).toRange(1.hours),
            listOf(
                now.toRange(2.hours)
            ),
        ),
        row(
            "Too far on the left side",
            now.toRange(3.hours),
            now.minus(2.hours).toRange(2.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        row(
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
})
