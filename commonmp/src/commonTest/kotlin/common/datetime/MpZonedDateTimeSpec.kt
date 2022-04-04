package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.datetime.TestConstants.tsBucharest_20220405_121314
import de.peekandpoke.ultra.common.datetime.TestConstants.tsUtc_20220405_121314
import de.peekandpoke.ultra.common.datetime.kotlinx.offsetMillisAt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours

@Suppress("unused")
class MpZonedDateTimeSpec : StringSpec({

    "Construction - parse(isoString, timezone)" {

        val bucharest = MpZonedDateTime.parse("2022-04-05T03:00", TimeZone.of("Europe/Bucharest"))
        val berlin = MpZonedDateTime.parse("2022-04-05T02:00", TimeZone.of("Europe/Berlin"))
        val utc = MpZonedDateTime.parse("2022-04-05T00:00", TimeZone.of("UTC"))

        bucharest.toEpochMillis() shouldBe utc.toEpochMillis()
        berlin.toEpochMillis() shouldBe utc.toEpochMillis()
    }

    "Construction - parse(isoString)" {

        val utc = MpZonedDateTime.parse("2022-04-05T00:00:00.000", TimeZone.UTC)

        val parsedUtcWithZ = MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")

        parsedUtcWithZ.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtcWithZ.timezone shouldBe TimeZone.UTC

        val parsedUtc = MpZonedDateTime.parse("2022-04-05T00:00:00.000[UTC]")

        parsedUtc.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtc.timezone shouldBe TimeZone.UTC

        val parsedEuropeBucharest = MpZonedDateTime.parse("2022-04-05T03:00:00.000[Europe/Bucharest]")

        parsedEuropeBucharest.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedEuropeBucharest.timezone shouldBe TimeZone.of("Europe/Bucharest")
    }

    "Equality" {

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeEqualComparingTo
                MpZonedDateTime.parse("2022-04-05T00:00:00Z")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeLessThan
                MpZonedDateTime.parse("2022-04-05T00:00:01Z")

        MpZonedDateTime.parse("2022-04-05T00:00:01Z") shouldBeGreaterThan
                MpZonedDateTime.parse("2022-04-05T00:00:00Z")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeEqualComparingTo
                MpZonedDateTime.parse("2022-04-05T03:00:00[Europe/Bucharest]")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeGreaterThan
                MpZonedDateTime.parse("2022-04-05T00:00:00[Europe/Bucharest]")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeLessThan
                MpZonedDateTime.parse("2022-04-05T00:00:00[US/Pacific]")
    }

    "toString" {
        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.UTC)
            .toString() shouldBe "MpZonedDateTime(2022-04-05T00:00:00.000Z)"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Bucharest"))
            .toString() shouldBe "MpZonedDateTime(2022-04-05T00:00:00.000[Europe/Bucharest])"
    }

    "toIsoString" {

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.UTC)
            .toIsoString() shouldBe "2022-04-05T00:00:00.000Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("UTC"))
            .toIsoString() shouldBe "2022-04-05T00:00:00.000Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Berlin"))
            .toIsoString() shouldBe "2022-04-05T00:00:00.000[Europe/Berlin]"
    }

    "parse - toIsoString - round trip" {
        val source = MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Bucharest"))

        val result = MpZonedDateTime.parse(source.toIsoString())

        result shouldBe source
    }

    "Fields year, monthNumber, month, dayOfMonth, dayOfWeek, dayOfYear, hour, minute, second, nano" {

        val subject = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14, 15)
            .atZone(TimeZone.of("Europe/Bucharest"))

        subject.year shouldBe 2022
        subject.monthNumber shouldBe 4
        subject.month shouldBe Month.APRIL
        subject.dayOfMonth shouldBe 5
        subject.dayOfWeek shouldBe DayOfWeek.TUESDAY
        subject.dayOfYear shouldBe 95

        subject.hour shouldBe 12
        subject.minute shouldBe 13
        subject.second shouldBe 14
        subject.nanosecond shouldBe 15
    }

    "toInstant - UTC" {
        val subject = MpZonedDateTime.parse("2022-04-05T12:13:14Z")

        val result = subject.toInstant()

        result.toEpochMillis() shouldBe tsUtc_20220405_121314

        result shouldBe MpInstant.parse("2022-04-05T12:13:14Z")
    }

    "toInstant - Europe/Bucharest" {
        val subject = MpZonedDateTime.parse("2022-04-05T12:13:14[Europe/Bucharest]")

        val result = subject.toInstant()

        result.toEpochMillis() shouldBe
                (tsUtc_20220405_121314 - TimeZone.of("Europe/Bucharest").offsetMillisAt(result))

        result shouldBe MpInstant.parse("2022-04-05T09:13:14Z")
    }

    "toLocalDate - UTC" {
        val timezone = TimeZone.UTC

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalDate() shouldBe MpLocalDate.parse("2022-04-05")

        MpZonedDateTime.parse("2022-04-05T23:59:59", timezone)
            .toLocalDate() shouldBe MpLocalDate.parse("2022-04-05")
    }

    "toLocalDate - Europe/Bucharest" {
        val timezone = TimeZone.of("Europe/Bucharest")

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalDate() shouldBe MpLocalDate.parse("2022-04-05")

        MpZonedDateTime.parse("2022-04-05T23:59:59.999Z", timezone)
            .toLocalDate() shouldBe MpLocalDate.parse("2022-04-05")
    }

    "toLocalDateTime - UTC" {
        val timezone = TimeZone.UTC

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T00:00:00")

        MpZonedDateTime.parse("2022-04-05T23:59:59", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T23:59:59")
    }

    "toLocalDateTime - Europe/Bucharest" {
        val timezone = TimeZone.of("Europe/Bucharest")

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T00:00:00")

        MpZonedDateTime.parse("2022-04-05T23:59:59", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T23:59:59")
    }

    "toEpochMillis" {

        MpZonedDateTime.parse("2022-04-05T12:13:14Z")
            .toEpochMillis() shouldBe tsUtc_20220405_121314

        MpZonedDateTime.parse("2022-04-05T12:13:14[Europe/Bucharest]")
            .toEpochMillis() shouldBe tsBucharest_20220405_121314
    }

    "toEpochSeconds" {
        MpZonedDateTime.parse("2022-04-05T12:13:14Z")
            .toEpochSeconds() shouldBe (tsUtc_20220405_121314 / 1000)

        MpZonedDateTime.parse("2022-04-05T12:13:14[Europe/Bucharest]")
            .toEpochSeconds() shouldBe (tsBucharest_20220405_121314 / 1000)
    }

    "plus(duration)" {
        val result = MpZonedDateTime.parse("2022-04-05T12:13:14Z").plus(1.hours)

        result shouldBe MpZonedDateTime.parse("2022-04-05T13:13:14Z")

        val resultBucharest = MpZonedDateTime.parse("2022-04-05T12:13:14[Europe/Bucharest]").plus(1.hours)

        resultBucharest shouldBe MpZonedDateTime.parse("2022-04-05T13:13:14[Europe/Bucharest]")
    }

    "minus(duration)" {
        val result = MpZonedDateTime.parse("2022-04-05T12:13:14Z").plus(1.hours)

        result shouldBe MpZonedDateTime.parse("2022-04-05T13:13:14Z")

        val resultBucharest = MpZonedDateTime.parse("2022-04-05T12:13:14[Europe/Bucharest]").minus(1.hours)

        resultBucharest shouldBe MpZonedDateTime.parse("2022-04-05T11:13:14[Europe/Bucharest]")
    }
})

