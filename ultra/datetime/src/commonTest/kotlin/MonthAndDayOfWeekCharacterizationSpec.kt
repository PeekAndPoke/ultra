package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * ULTRA-00210: what `month`, `dayOfWeek` and the zone offset return. Written against `name` and `ordinal` only, so the
 * same expectations hold for the kotlinx return types before the change and the Mp types after it.
 */
@Suppress("unused")
class MonthAndDayOfWeekCharacterizationSpec : StringSpec({

    val berlin = MpTimezone.of("Europe/Berlin")

    // date to (month, day of week), proleptic Gregorian
    listOf(
        MpLocalDate.of(1, 1, 1) to ("JANUARY" to "MONDAY"),
        MpLocalDate.of(1582, 10, 15) to ("OCTOBER" to "FRIDAY"),
        MpLocalDate.of(1970, 1, 1) to ("JANUARY" to "THURSDAY"),
        MpLocalDate.of(2000, 1, 1) to ("JANUARY" to "SATURDAY"),
        MpLocalDate.of(2022, 4, 5) to ("APRIL" to "TUESDAY"),
        MpLocalDate.of(2022, 12, 31) to ("DECEMBER" to "SATURDAY"),
        MpLocalDate.of(2024, 2, 29) to ("FEBRUARY" to "THURSDAY"),
        MpLocalDate.of(9999, 12, 31) to ("DECEMBER" to "FRIDAY"),
    ).forEach { (date, expected) ->
        val (month, dayOfWeek) = expected

        "$date: MpLocalDate, MpLocalDateTime and MpZonedDateTime return $month and $dayOfWeek" {
            val dateTime = date.atTime(MpLocalTime.of(12, 0))
            val zoned = dateTime.atZone(berlin)

            listOf(date.month.name, dateTime.month.name, zoned.month.name) shouldBe listOf(month, month, month)
            listOf(date.dayOfWeek.name, dateTime.dayOfWeek.name, zoned.dayOfWeek.name) shouldBe
                    listOf(dayOfWeek, dayOfWeek, dayOfWeek)
        }
    }

    "every day of 2024: month.ordinal + 1 is the month number, the day of week advances by one" {
        var date = MpLocalDate.of(2024, 1, 1)
        var expectedDayOfWeek = 0 // MONDAY

        while (date.year == 2024) {
            date.month.ordinal + 1 shouldBe date.monthNumber
            date.dayOfWeek.ordinal shouldBe expectedDayOfWeek

            date = date.plusDays(1)
            expectedDayOfWeek = (expectedDayOfWeek + 1) % 7
        }
    }

    "the zone offset is read as plain numbers: winter, summer, UTC" {
        val winter = MpInstant.parse("2022-01-15T12:00:00Z")
        val summer = MpInstant.parse("2022-07-15T12:00:00Z")

        berlin.offsetSecondsAt(winter) shouldBe 3600
        berlin.offsetSecondsAt(summer) shouldBe 7200
        berlin.offsetMillisAt(summer) shouldBe 7_200_000L
        MpTimezone.UTC.offsetSecondsAt(summer) shouldBe 0
    }
})
