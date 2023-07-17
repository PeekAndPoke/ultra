package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

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

        MpLocalDateTime.Genesis.toIsoString() shouldBe "-10000-01-01T00:00:00.000Z"

        MpLocalDateTime.Doomsday shouldBe MpLocalDateTime.of(10000, Month.JANUARY, 1)

        MpLocalDateTime.Doomsday.toIsoString() shouldBe "+10000-01-01T00:00:00.000Z"
    }

    "Equality" {

        MpLocalDateTime.parse("2022-04-01T12:00:00.000") shouldBeEqualComparingTo
                MpLocalDateTime.parse("2022-04-01T12:00:00.000")

        MpLocalDateTime.parse("2022-04-01T12:00:00.000") shouldBeLessThan
                MpLocalDateTime.parse("2022-04-02T12:00:00.000")

        MpLocalDateTime.parse("2022-04-02T12:00:00.000") shouldBeGreaterThan
                MpLocalDateTime.parse("2022-04-01T12:00:00.000")
    }

    "toString" {
        MpLocalDateTime.parse("2022-04-02T12:00")
            .toString() shouldBe "MpLocalDateTime(2022-04-02T12:00:00.000Z)"
    }

    "toIsoString" {
        MpLocalDateTime.parse("2022-04-02T12:00")
            .toIsoString() shouldBe "2022-04-02T12:00:00.000Z"
    }

    "parse - toIsoString - round trip" {
        val start = MpLocalDateTime.parse("2022-04-01T00:00:00Z")

        val result = MpLocalDateTime.parse(start.toIsoString())

        start shouldBe result
    }

    "parse - should throw on invalid input" {
        shouldThrow<IllegalArgumentException> {
            MpLocalDateTime.parse("")
        }
    }

    "tryParse - should return null invalid input" {
        MpLocalDateTime.tryParse("") shouldBe null
    }

    "Fields year, monthNumber, month, dayOfMonth, dayOfWeek, dayOfYear, hour, minute, second, milliSecond" {

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
        subject.milliSecond shouldBe 15
    }

    "toDate" {
        val subject = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14, 15)

        subject.toDate() shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "toTime()" {
        val inputs = listOf(
            MpLocalTime.of(hour = 12, minute = 13, second = 14, milliSecond = 0),
            MpLocalTime.of(hour = 12, minute = 13, second = 14, milliSecond = 15),
            MpLocalTime.of(hour = 12, minute = 13, second = 14, milliSecond = 999),
        )

        inputs.forEach { time ->
            withClue("Must work for '$time'") {
                val subject = MpLocalDateTime.of(
                    MpLocalDate.of(2022, Month.APRIL, 5),
                    time
                )

                subject.toTime() shouldBe time
            }
        }
    }

    "toInstant" {
        val source = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)

        val expectedTs = TestConstants.tsUtc_20220405_121314

        val instantUTC = source.toInstant(TimeZone.UTC)

        withClue("at TimeZone.UTC must not shift the timestamp") {
            instantUTC shouldBe MpInstant.parse("2022-04-05T12:13:14.000Z")
            instantUTC.toEpochMillis() shouldBe expectedTs
        }

        withClue("at TimeZone Europe/Bucharest must shift the timestamp") {
            val timezone = TimeZone.of("Europe/Bucharest")
            val instantBucharest = source.toInstant(timezone)
            instantBucharest shouldBe MpInstant.parse("2022-04-05T09:13:14.000Z")
            instantBucharest.toEpochMillis() shouldBe
                    (expectedTs - timezone.offsetMillisAt(instantUTC))
        }
    }

    "atZone ... must shift the timestamp with respect to the target timezone" {

        val source = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
        val sourceInstant = source.toInstant(TimeZone.UTC)

        val expectedTs = TestConstants.tsUtc_20220405_121314

        val zones = listOf(
            TimeZone.UTC,
            TimeZone.of("UTC"),
            TimeZone.of("Europe/Bucharest")
        )

        zones.forEach { timezone ->

            withClue("Must work for timezone '${timezone.id}'") {

                val result = source.atZone(timezone)

                result shouldBe MpZonedDateTime.of(source, timezone)

                result.toEpochMillis() shouldBe
                        (expectedTs - timezone.offsetMillisAt(sourceInstant))
            }
        }
    }

    "atUTC" {
        val source = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)

        source.atUTC() shouldBe source.atZone(TimeZone.UTC)

        source.atUTC() shouldBe source.atZone(TimeZone.of("UTC"))
    }
})
