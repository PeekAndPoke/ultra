package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

@Suppress("unused")
class ExtensionsSpec : StringSpec({

    "UtcOffset.totalMillis" {

        TimeZone.UTC.offset.totalMillis shouldBe 0

        TimeZone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ).totalMillis shouldBe 1 * 60 * 60 * 1_000

        TimeZone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ).totalMillis shouldBe 2 * 60 * 60 * 1_000
    }

    "TimeZone.offsetAt" {

        TimeZone.UTC.offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 0)

        TimeZone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 1)

        TimeZone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 2)
    }

    "TimeZone.offsetSecondsAt" {

        TimeZone.UTC.offsetSecondsAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 0

        TimeZone.of("Europe/Berlin").offsetSecondsAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 1 * 60 * 60

        TimeZone.of("Europe/Berlin").offsetSecondsAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe 2 * 60 * 60
    }

    "TimeZone.offsetMillisAt" {

        TimeZone.UTC.offsetMillisAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 0

        TimeZone.of("Europe/Berlin").offsetMillisAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 1 * 60 * 60 * 1_000

        TimeZone.of("Europe/Berlin").offsetMillisAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe 2 * 60 * 60 * 1_000
    }

    "MpTimeZone.offsetAt" {

        MpTimezone.UTC.offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 0)

        MpTimezone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 1)

        MpTimezone.of("Europe/Berlin").offsetAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 2)
    }

    "MpTimeZone.offsetSecondsAt" {

        MpTimezone.UTC.offsetSecondsAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 0

        MpTimezone.of("Europe/Berlin").offsetSecondsAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 1 * 60 * 60

        MpTimezone.of("Europe/Berlin").offsetSecondsAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe 2 * 60 * 60
    }

    "MpTimeZone.offsetMillisAt" {

        MpTimezone.UTC.offsetMillisAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 0

        MpTimezone.of("Europe/Berlin").offsetMillisAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe 1 * 60 * 60 * 1_000

        MpTimezone.of("Europe/Berlin").offsetMillisAt(
            MpInstant.parse("2022-03-28T00:00:00.000Z")
        ) shouldBe 2 * 60 * 60 * 1_000
    }
})
