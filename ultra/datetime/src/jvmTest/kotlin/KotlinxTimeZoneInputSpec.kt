package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

/**
 * Named kotlinx [TimeZone]s as INPUTS. JVM only: on Kotlin/JS kotlinx cannot create a region zone without the js-joda
 * tz database, which ultra deliberately does not ship.
 */
@Suppress("unused")
class KotlinxTimeZoneInputSpec : StringSpec({

    "MpTimezone.kotlinx converts to a kotlinx TimeZone" {
        val kotlinxTz = MpTimezone.of("Europe/Berlin").kotlinx

        kotlinxTz.shouldBeInstanceOf<TimeZone>()
        kotlinxTz.id shouldBe "Europe/Berlin"
    }

    MpTimezone.supportedIds
        .filter { it != "Z" }
        .forEach { timezoneId ->
            "MpTimezone '$timezoneId' converts to a kotlinx TimeZone" {
                MpTimezone.of(timezoneId).kotlinx.id shouldBe timezoneId
            }
        }

    "TimeZone.mp" {
        val kotlinxTz = TimeZone.of("Europe/Berlin")
        val mpTz = kotlinxTz.mp

        mpTz shouldBe MpTimezone.of("Europe/Berlin")
        mpTz.id shouldBe "Europe/Berlin"

        TimeZone.UTC.mp shouldBe MpTimezone.of("UTC")
    }

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

    "TimeZone overloads give the same result as the MpTimezone overloads" {
        val kotlinxTz = TimeZone.of("Europe/Berlin")
        val mpTz = MpTimezone.of("Europe/Berlin")
        val local = MpLocalDateTime.of(2022, 3, 27, 2, 30)

        local.toInstant(kotlinxTz) shouldBe local.toInstant(mpTz)
        local.atZone(kotlinxTz) shouldBe local.atZone(mpTz)
        local.toDate().atStartOfDay(kotlinxTz) shouldBe local.toDate().atStartOfDay(mpTz)
        MpZonedDateTime.parse("2022-03-27T02:30:00", kotlinxTz) shouldBe MpZonedDateTime.parse("2022-03-27T02:30:00", mpTz)
    }
})
