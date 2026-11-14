package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

/**
 * ULTRA-00211 (maintainer, 2026-11-14): `atStartOfNext` / `atStartOfPrevious` step on the date, so a day the zone skips
 * (Samoa and Tokelau skipped 2011-12-30) cannot loop forever. Every case runs under a time limit.
 */
@Suppress("unused")
class MpZonedDateTimeWeekdaySpec : StringSpec({

    val apia = MpTimezone.of("Pacific/Apia") // 2011-12-29 at -10:00, then 2011-12-31 at +14:00
    val fakaofo = MpTimezone.of("Pacific/Fakaofo") // 2011-12-29 at -11:00, then 2011-12-31 at +13:00

    data class Case(
        val title: String,
        val call: () -> MpZonedDateTime,
        val datetime: MpLocalDateTime,
        val instant: String,
    )

    fun at(zone: MpTimezone, y: Int, m: Int, d: Int, h: Int) =
        MpZonedDateTime.of(MpLocalDateTime.of(y, m, d, h, 0), zone)

    val apiaSat = at(apia, 2011, 12, 31, 12)
    val apiaTue = at(apia, 2012, 1, 3, 12)
    val apiaThu = at(apia, 2011, 12, 29, 12)
    val fakaofoSat = at(fakaofo, 2011, 12, 31, 12)

    listOf(
        // the skipped Friday resolves like atStartOfDay of that date: the end of the gap, the start of 12-31
        Case("Apia 2011-12-31T12:00, previous FRIDAY", { apiaSat.atStartOfPrevious(MpDayOfWeek.FRIDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2011-12-31T12:00, previous SATURDAY", { apiaSat.atStartOfPrevious(MpDayOfWeek.SATURDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2011-12-31T12:00, previous THURSDAY", { apiaSat.atStartOfPrevious(MpDayOfWeek.THURSDAY) },
            MpLocalDateTime.of(2011, 12, 29, 0, 0), "2011-12-29T10:00:00Z"),
        Case("Apia 2012-01-03T12:00, previous FRIDAY", { apiaTue.atStartOfPrevious(MpDayOfWeek.FRIDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2012-01-03T12:00, previous SATURDAY", { apiaTue.atStartOfPrevious(MpDayOfWeek.SATURDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2012-01-03T12:00, previous THURSDAY", { apiaTue.atStartOfPrevious(MpDayOfWeek.THURSDAY) },
            MpLocalDateTime.of(2011, 12, 29, 0, 0), "2011-12-29T10:00:00Z"),
        Case("Fakaofo 2011-12-31T12:00, previous FRIDAY", { fakaofoSat.atStartOfPrevious(MpDayOfWeek.FRIDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T11:00:00Z"),
        Case("Fakaofo 2011-12-31T12:00, previous THURSDAY", { fakaofoSat.atStartOfPrevious(MpDayOfWeek.THURSDAY) },
            MpLocalDateTime.of(2011, 12, 29, 0, 0), "2011-12-29T11:00:00Z"),
        Case("Apia 2011-12-29T12:00, next FRIDAY", { apiaThu.atStartOfNext(MpDayOfWeek.FRIDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2011-12-29T12:00, next SATURDAY", { apiaThu.atStartOfNext(MpDayOfWeek.SATURDAY) },
            MpLocalDateTime.of(2011, 12, 31, 0, 0), "2011-12-30T10:00:00Z"),
        Case("Apia 2011-12-29T12:00, next SUNDAY", { apiaThu.atStartOfNext(MpDayOfWeek.SUNDAY) },
            MpLocalDateTime.of(2012, 1, 1, 0, 0), "2011-12-31T10:00:00Z"),
    ).forEach { case ->
        "${case.title} terminates at ${case.datetime}" {
            val result = withinTimeLimit(5.seconds) { case.call() }

            result.datetime shouldBe case.datetime
            result.toInstant() shouldBe MpInstant.parse(case.instant)
        }
    }

    // Europe/Berlin across both 2022 DST switches: the results of the code before the fix, pinned
    data class Row(val start: String, val day: MpDayOfWeek, val next: Long, val previous: Long)

    val berlin = MpTimezone.of("Europe/Berlin")

    val starts = mapOf(
        "2022-03-26T12:00" to at(berlin, 2022, 3, 26, 12),
        "2022-03-27T01:30" to MpZonedDateTime.of(MpLocalDateTime.of(2022, 3, 27, 1, 30), berlin),
        "2022-03-27T03:30" to MpZonedDateTime.of(MpLocalDateTime.of(2022, 3, 27, 3, 30), berlin),
        "2022-03-28T00:30" to MpZonedDateTime.of(MpLocalDateTime.of(2022, 3, 28, 0, 30), berlin),
        "2022-10-29T12:00" to at(berlin, 2022, 10, 29, 12),
        "2022-10-30T02:30+02" to MpInstant.fromEpochMillis(1_667_089_800_000L).atZone(berlin),
        "2022-10-30T02:30+01" to MpInstant.fromEpochMillis(1_667_093_400_000L).atZone(berlin),
        "2022-10-30T12:00" to at(berlin, 2022, 10, 30, 12),
        "2022-10-31T00:00" to at(berlin, 2022, 10, 31, 0),
    )

    listOf(
        Row("2022-03-26T12:00", MpDayOfWeek.MONDAY, next = 1648418400000L, previous = 1647817200000L),
        Row("2022-03-26T12:00", MpDayOfWeek.SATURDAY, next = 1648850400000L, previous = 1648249200000L),
        Row("2022-03-26T12:00", MpDayOfWeek.SUNDAY, next = 1648335600000L, previous = 1647730800000L),
        Row("2022-03-27T01:30", MpDayOfWeek.MONDAY, next = 1648418400000L, previous = 1647817200000L),
        Row("2022-03-27T01:30", MpDayOfWeek.SATURDAY, next = 1648850400000L, previous = 1648249200000L),
        Row("2022-03-27T01:30", MpDayOfWeek.SUNDAY, next = 1648936800000L, previous = 1648335600000L),
        Row("2022-03-27T03:30", MpDayOfWeek.MONDAY, next = 1648418400000L, previous = 1647817200000L),
        Row("2022-03-27T03:30", MpDayOfWeek.SATURDAY, next = 1648850400000L, previous = 1648249200000L),
        Row("2022-03-27T03:30", MpDayOfWeek.SUNDAY, next = 1648936800000L, previous = 1648335600000L),
        Row("2022-03-28T00:30", MpDayOfWeek.MONDAY, next = 1649023200000L, previous = 1648418400000L),
        Row("2022-03-28T00:30", MpDayOfWeek.SATURDAY, next = 1648850400000L, previous = 1648249200000L),
        Row("2022-03-28T00:30", MpDayOfWeek.SUNDAY, next = 1648936800000L, previous = 1648335600000L),
        Row("2022-10-29T12:00", MpDayOfWeek.MONDAY, next = 1667170800000L, previous = 1666562400000L),
        Row("2022-10-29T12:00", MpDayOfWeek.SATURDAY, next = 1667602800000L, previous = 1666994400000L),
        Row("2022-10-29T12:00", MpDayOfWeek.SUNDAY, next = 1667080800000L, previous = 1666476000000L),
        Row("2022-10-30T02:30+02", MpDayOfWeek.MONDAY, next = 1667170800000L, previous = 1666562400000L),
        Row("2022-10-30T02:30+02", MpDayOfWeek.SATURDAY, next = 1667602800000L, previous = 1666994400000L),
        Row("2022-10-30T02:30+02", MpDayOfWeek.SUNDAY, next = 1667689200000L, previous = 1667080800000L),
        Row("2022-10-30T02:30+01", MpDayOfWeek.MONDAY, next = 1667170800000L, previous = 1666562400000L),
        Row("2022-10-30T02:30+01", MpDayOfWeek.SATURDAY, next = 1667602800000L, previous = 1666994400000L),
        Row("2022-10-30T02:30+01", MpDayOfWeek.SUNDAY, next = 1667689200000L, previous = 1667080800000L),
        Row("2022-10-30T12:00", MpDayOfWeek.MONDAY, next = 1667170800000L, previous = 1666562400000L),
        Row("2022-10-30T12:00", MpDayOfWeek.SATURDAY, next = 1667602800000L, previous = 1666994400000L),
        Row("2022-10-30T12:00", MpDayOfWeek.SUNDAY, next = 1667689200000L, previous = 1667080800000L),
        Row("2022-10-31T00:00", MpDayOfWeek.MONDAY, next = 1667775600000L, previous = 1667170800000L),
        Row("2022-10-31T00:00", MpDayOfWeek.SATURDAY, next = 1667602800000L, previous = 1666994400000L),
        Row("2022-10-31T00:00", MpDayOfWeek.SUNDAY, next = 1667689200000L, previous = 1667080800000L),
    ).forEach { row ->
        "Berlin ${row.start}, ${row.day}: next and previous as before the fix" {
            val start = starts.getValue(row.start)

            withinTimeLimit(5.seconds) { start.atStartOfNext(row.day) }.toEpochMillis() shouldBe row.next
            withinTimeLimit(5.seconds) { start.atStartOfPrevious(row.day) }.toEpochMillis() shouldBe row.previous
            start.atStartOfNext(row.day.kotlinx) shouldBe start.atStartOfNext(row.day)
            start.atStartOfPrevious(row.day.kotlinx) shouldBe start.atStartOfPrevious(row.day)
        }
    }
})
