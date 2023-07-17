package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.datetime.TestConstants.tsBucharest_20220405_121314
import de.peekandpoke.ultra.common.datetime.TestConstants.tsUtc_20220405_121314
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class MpZonedDateTimeSpec : StringSpec({

    "Construction - parse(isoString, timezone)" {

        val utc = MpZonedDateTime.parse("2022-04-05T00:00", TimeZone.of("UTC"))

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T00:00",
            timezone = TimeZone.of("Europe/Bucharest")
        )

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T00:00:00.000Z",
            timezone = TimeZone.of("Europe/Bucharest")
        )

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T03:00:00[Europe/Bucharest]",
            timezone = TimeZone.of("Europe/Bucharest")
        )

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T00:00",
            timezone = TimeZone.of("Europe/Berlin")
        )

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T02:00:00[Europe/Berlin]",
            timezone = TimeZone.of("Europe/Berlin")
        )

        utc shouldBeEqualComparingTo MpZonedDateTime.parse(
            isoString = "2022-04-05T02:00:00[Europe/Berlin]",
            timezone = TimeZone.of("Europe/Bucharest")
        )

        MpZonedDateTime.parse(
            isoString = "2022-04-05T02:00:00[Europe/Berlin]",
            timezone = TimeZone.of("Europe/Bucharest")
        ).toIsoString() shouldBe "2022-04-05T03:00:00.000[Europe/Bucharest]"

        MpZonedDateTime.parse(
            isoString = "2022-04-05T02:00:00[Europe/Bucharest]",
            timezone = TimeZone.of("Europe/Berlin")
        ).toIsoString() shouldBe "2022-04-05T01:00:00.000[Europe/Berlin]"
    }

    "Construction - parse(isoString)" {

        val utc = MpZonedDateTime.parse("2022-04-05T00:00:00.000", TimeZone.UTC)
        val parsedUtcWithZ = MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")

        parsedUtcWithZ.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtcWithZ.timezone shouldBe MpTimezone.UTC

        val parsedUtc = MpZonedDateTime.parse("2022-04-05T00:00:00.000[UTC]")

        parsedUtc.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtc.timezone shouldBe MpTimezone.UTC

        val parsedEuropeBucharest = MpZonedDateTime.parse("2022-04-05T03:00:00.000[Europe/Bucharest]")

        parsedEuropeBucharest.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedEuropeBucharest.timezone shouldBe MpTimezone.of("Europe/Bucharest")

        shouldThrow<IllegalArgumentException> {
            MpZonedDateTime.parse("")
        }
    }

    "Construction - tryParse(isoString)" {

        val utc = MpZonedDateTime.tryParse("2022-04-05T00:00:00.000", TimeZone.UTC)
        val parsedUtcWithZ = MpZonedDateTime.tryParse("2022-04-05T00:00:00.000Z")

        parsedUtcWithZ.shouldNotBeNull()
        parsedUtcWithZ.toEpochMillis() shouldBe utc!!.toEpochMillis()
        parsedUtcWithZ.timezone shouldBe MpTimezone.UTC

        val parsedUtc = MpZonedDateTime.tryParse("2022-04-05T00:00:00.000[UTC]")

        parsedUtc.shouldNotBeNull()
        parsedUtc.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtc.timezone shouldBe MpTimezone.UTC

        MpZonedDateTime.tryParse("") shouldBe null
    }

    "Construction from epoch" {
        MpZonedDateTime.fromEpochMillis(TestConstants.tsUTC_20220405_000000, TimeZone.UTC) shouldBe
                MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")

        MpZonedDateTime.fromEpochSeconds(
            seconds = TestConstants.tsUTC_20220405_000000 / 1_000,
            timezone = TimeZone.of("Europe/Bucharest")
        ) shouldBe MpZonedDateTime.parse("2022-04-05T03:00:00.000[Europe/Bucharest]")
    }

    "Genesis and Doomsday" {
        MpZonedDateTime.Genesis.toEpochMillis() shouldBe GENESIS_TIMESTAMP

        MpZonedDateTime.Genesis.toIsoString() shouldBe "-10000-01-01T00:00:00.000Z"

        MpZonedDateTime.Doomsday.toEpochMillis() shouldBe DOOMSDAY_TIMESTAMP

        MpZonedDateTime.Doomsday.toIsoString() shouldBe "+10000-01-01T00:00:00.000Z"
    }

    "Comparison" {

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
            .toString() shouldBe "MpZonedDateTime(2022-04-05T03:00:00.000[Europe/Bucharest])"
    }

    "toIsoString" {

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.UTC)
            .toIsoString() shouldBe "2022-04-05T00:00:00.000Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("UTC"))
            .toIsoString() shouldBe "2022-04-05T00:00:00.000Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Berlin"))
            .toIsoString() shouldBe "2022-04-05T02:00:00.000[Europe/Berlin]"
    }

    "parse - toIsoString - round trip" {
        val source = MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Bucharest"))

        val result = MpZonedDateTime.parse(source.toIsoString())

        result shouldBe source
    }

    "format" {
        MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
            .format("yyyy-MM-dd'T'HH:mm:ss.SSSZ") shouldBe "2022-04-05T00:00:00.000Z"

        MpZonedDateTime.parse("2022-04-05T12:00:00.000[Europe/Bucharest]")
            .format("yyyy-MM-dd HH:mm") shouldBe "2022-04-05 12:00"

        MpZonedDateTime.parse("2022-04-05T12:00:00.000[US/Pacific]")
            .format("yyyy-MM-dd HH:mm") shouldBe "2022-04-05 12:00"
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
        subject.milliSecond shouldBe 15
    }

    "Fields dayOfWeek" {
        val subject = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14, 15)
            .atZone(TimeZone.of("Europe/Bucharest"))

        subject.dayOfWeek shouldBe DayOfWeek.TUESDAY
    }

    "Fields dayOfYear" {
        val subject = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14, 15)
            .atZone(TimeZone.of("Europe/Bucharest"))

        subject.dayOfYear shouldBe 95
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
            .toLocalDate() shouldBe MpLocalDate.parse("2022-04-06")
    }

    "toLocalTime - UTC" {
        val timezone = TimeZone.UTC

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalTime() shouldBe MpLocalTime.of(hour = 0, minute = 0, second = 0, milliSecond = 0)

        MpZonedDateTime.parse("2022-04-05T23:59:59.999", timezone)
            .toLocalTime() shouldBe MpLocalTime.of(hour = 23, minute = 59, second = 59, milliSecond = 999)
    }

    "toLocalTime - Europe/Bucharest" {
        val timezone = TimeZone.of("Europe/Bucharest")

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalTime() shouldBe MpLocalTime.of(hour = 3, minute = 0, second = 0, milliSecond = 0)

        MpZonedDateTime.parse("2022-04-05T23:59:59.999Z", timezone)
            .toLocalTime() shouldBe MpLocalTime.of(hour = 2, minute = 59, second = 59, milliSecond = 999)
    }

    "toLocalDateTime - UTC" {
        val timezone = TimeZone.UTC

        MpZonedDateTime.parse("2022-04-05T00:00:00", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T00:00:00")

        MpZonedDateTime.parse("2022-04-05T12:13:14.123", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-05T12:13:14.123")
    }

    "toLocalDateTime - Europe/Bucharest" {
        val timezone = TimeZone.of("Europe/Bucharest")

        MpZonedDateTime.parse("2022-04-05T22:00:00", timezone)
            .toLocalDateTime() shouldBe MpLocalDateTime.parse("2022-04-06T01:00:00")
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

    "atZone - UTC to Europe/Bucharest" {

        val source = MpZonedDateTime.parse("2022-04-05T12:13:14.123Z")

        val timezone = TimeZone.of("Europe/Bucharest")
        val atZone = source.atZone(timezone)

        atZone.timezone shouldBe timezone.mp
        atZone.toEpochMillis() shouldBe source.toEpochMillis()

        atZone.toIsoString() shouldBe "2022-04-05T15:13:14.123[Europe/Bucharest]"
    }

    "atZone - same timezone" {

        val timezone = TimeZone.of("Europe/Bucharest")

        val source = MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]")
        source.timezone shouldBe timezone.mp

        val atZone = source.atZone(timezone)

        atZone.timezone shouldBe timezone.mp
        atZone.toEpochMillis() shouldBe source.toEpochMillis()

        atZone.toIsoString() shouldBe "2022-04-05T12:13:14.123[Europe/Bucharest]"
    }

    "atStartOfYear" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfYear().let {
            it shouldBe MpZonedDateTime.parse("2022-01-01T00:00:00.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfYear().let {
            it shouldBe MpZonedDateTime.parse("2022-01-01T00:00:00.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfMonth" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfMonth().let {
            it shouldBe MpZonedDateTime.parse("2022-04-01T00:00:00.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfMonth().let {
            it shouldBe MpZonedDateTime.parse("2022-04-01T00:00:00.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfDay" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfDay().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfDay().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T00:00:00.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfHour" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfHour().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:00:00.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfHour().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:00:00.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfMinute" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfMinute().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:13:00.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfMinute().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:13:00.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfSecond" {
        MpZonedDateTime.parse("2022-04-05T12:13:14.123Z").atStartOfSecond().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:13:14.000Z")
            it.timezone shouldBe MpTimezone.UTC
        }

        MpZonedDateTime.parse("2022-04-05T12:13:14.123[Europe/Bucharest]").atStartOfSecond().let {
            it shouldBe MpZonedDateTime.parse("2022-04-05T12:13:14.000[Europe/Bucharest]")
            it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
        }
    }

    "atStartOfNext(dayOfWeek)" {

        // Before DST in Berlin
        MpZonedDateTime.parse("2022-03-27T00:00:00.000[Europe/Berlin]")
            .atStartOfNext(DayOfWeek.MONDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-03-28T00:00:00.000[Europe/Berlin]")
                it.timezone shouldBe MpTimezone.of("Europe/Berlin")
            }

        MpZonedDateTime.parse("2022-04-04T23:59:59.999Z")
            .atStartOfNext(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
                it.timezone shouldBe MpTimezone.UTC
            }

        MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
            .atStartOfNext(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-04-12T00:00:00.000Z")
                it.timezone shouldBe MpTimezone.UTC
            }

        MpZonedDateTime.parse("2022-04-05T00:00:00.001[Europe/Bucharest]")
            .atStartOfNext(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-04-12T00:00:00.000[Europe/Bucharest]")
                it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
            }
    }

    "atStartOfPrevious(dayOfWeek)" {

        // After DST in Berlin
        MpZonedDateTime.parse("2022-03-28T00:00:00.000[Europe/Berlin]")
            .atStartOfPrevious(DayOfWeek.SUNDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-03-27T00:00:00.000[Europe/Berlin]")
                it.timezone shouldBe MpTimezone.of("Europe/Berlin")
            }

        MpZonedDateTime.parse("2022-04-04T23:59:59.999Z")
            .atStartOfPrevious(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-03-29T00:00:00.000Z")
                it.timezone shouldBe MpTimezone.UTC
            }

        MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
            .atStartOfPrevious(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")
                it.timezone shouldBe MpTimezone.UTC
            }

        MpZonedDateTime.parse("2022-04-05T00:00:00.001[Europe/Bucharest]")
            .atStartOfPrevious(DayOfWeek.TUESDAY).let {
                it shouldBe MpZonedDateTime.parse("2022-04-05T00:00:00.000[Europe/Bucharest]")
                it.timezone shouldBe MpTimezone.of("Europe/Bucharest")
            }
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

    "minus(other) -> Duration" {

        val source = MpZonedDateTime.parse("2022-04-05T12:00:00Z")

        val duration = 1.hours + 2.minutes

        (source.plus(duration) - source) shouldBe duration
        (source.minus(duration) - source) shouldBe duration.unaryMinus()
    }
})
