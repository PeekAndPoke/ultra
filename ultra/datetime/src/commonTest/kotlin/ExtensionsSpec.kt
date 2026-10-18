package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

@Suppress("unused")
class ExtensionsSpec : StringSpec({

    "TimeZone.mp" {
        TimeZone.UTC.mp shouldBe MpTimezone.of("UTC")
        TimeZone.of("+02:00").mp shouldBe MpTimezone.of("+02:00")
    }

    "TimeZone.offsetAt - fixed offsets need no zone data on any target" {
        TimeZone.UTC.offset.totalMillis shouldBe 0

        TimeZone.UTC.offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 0)

        TimeZone.of("+02:00").offsetAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe UtcOffset(hours = 2)

        TimeZone.of("-05:30").offsetSecondsAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
        ) shouldBe -(5 * 60 + 30) * 60

        TimeZone.of("+02:00").offsetMillisAt(
            MpInstant.parse("2022-03-27T00:00:00.000Z")
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
