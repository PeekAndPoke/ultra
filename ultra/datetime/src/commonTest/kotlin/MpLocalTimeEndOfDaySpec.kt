package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds

/** ULTRA-00316 track A batch 3a: `MpLocalTime.EndOfDay`, the time 24:00 (maintainer, 2026-11-14). */
@Suppress("unused")
class MpLocalTimeEndOfDaySpec : StringSpec({

    val endOfDay = MpLocalTime.EndOfDay

    "EndOfDay is the value one millisecond past Max; the unchecked constructor gives the same value" {
        endOfDay.inWholeMilliSeconds() shouldBe 86_400_000
        MpLocalTime(86_400_000) shouldBe endOfDay
        (MpLocalTime.Min < MpLocalTime.Max) shouldBe true
        (MpLocalTime.Max < endOfDay) shouldBe true
        endOfDay - MpLocalTime.Max shouldBe 1.milliseconds
        endOfDay - MpLocalTime.Min shouldBe 24.hours
    }

    "fields and text: 24:00:00.000" {
        listOf(endOfDay.hour, endOfDay.minute, endOfDay.second, endOfDay.milliSecond) shouldBe listOf(24, 0, 0, 0)
        endOfDay.toIsoString() shouldBe "24:00:00.000"
        endOfDay.toString() shouldBe "MpLocalTime(24:00:00.000)"
        endOfDay.format("HH") shouldBe "24"
        endOfDay.format("HH:mm") shouldBe "24:00"
        endOfDay.format("HH:mm:ss") shouldBe "24:00:00"
        endOfDay.format("HH:mm:ss.SSS") shouldBe "24:00:00.000"
        endOfDay.formatHhMm() shouldBe "24:00"
        endOfDay.formatHhMmSs() shouldBe "24:00:00"
    }

    listOf("24:00", "24:00:00", "24:00:00.000", "24:00:00.0").forEach { input ->
        "parse(\"$input\") is EndOfDay" {
            MpLocalTime.parse(input) shouldBe endOfDay
            MpLocalTime.tryParse(input) shouldBe endOfDay
        }
    }

    listOf("24:00:01", "24:30", "24:59", "24:00:00.001", "25:00").forEach { input ->
        "parse(\"$input\") is refused: IllegalArgumentException, tryParse null" {
            shouldThrow<IllegalArgumentException> { MpLocalTime.parse(input) }
            MpLocalTime.tryParse(input) shouldBe null
        }
    }

    "of(24, 0), ofMilliSeconds(86_400_000) and ofSecondOfDay(86_400) keep wrapping to 00:00" {
        MpLocalTime.of(24, 0) shouldBe MpLocalTime.Min
        MpLocalTime.ofMilliSeconds(86_400_000) shouldBe MpLocalTime.Min
        MpLocalTime.ofSecondOfDay(86_400) shouldBe MpLocalTime.Min
    }

    "arithmetic treats EndOfDay as 00:00 of the next day; adding nothing keeps it" {
        endOfDay.plus(Duration.ZERO) shouldBe endOfDay
        endOfDay.minus(Duration.ZERO) shouldBe endOfDay
        endOfDay.plus(1.hours) shouldBe MpLocalTime.of(1, 0)
        endOfDay.minus(1.hours) shouldBe MpLocalTime.of(23, 0)
        endOfDay.plus(24.hours) shouldBe MpLocalTime.Min
        MpLocalTime.of(23, 0).plus(1.hours) shouldBe MpLocalTime.Min
    }

    "kotlinx: EndOfDay converts to the last instant of the day; the reverse never gives EndOfDay" {
        endOfDay.kotlinx shouldBe LocalTime(23, 59, 59, 999_999_999)
        endOfDay.kotlinx.mp shouldBe MpLocalTime.Max
    }

    "kotlinx Json: written as 86400000, read back as EndOfDay; every other out-of-range number still wraps" {
        Json.encodeToString(MpLocalTime.serializer(), endOfDay) shouldBe "86400000"
        Json.decodeFromString(MpLocalTime.serializer(), "86400000") shouldBe endOfDay
        Json.decodeFromString(MpLocalTime.serializer(), "86400001") shouldBe MpLocalTime.of(0, 0, 0, 1)
        Json.decodeFromString(MpLocalTime.serializer(), "172800000") shouldBe MpLocalTime.Min
        Json.decodeFromString(MpLocalTime.serializer(), "-86400000") shouldBe MpLocalTime.Min
    }

    "MpLocalDate.atTime(EndOfDay) and MpLocalDateTime.of(date, EndOfDay) are 00:00 of the next day" {
        val date = MpLocalDate.of(2024, 3, 30)

        date.atTime(endOfDay) shouldBe MpLocalDateTime.of(2024, 3, 31, 0, 0)
        MpLocalDateTime.of(date, endOfDay) shouldBe MpLocalDateTime.of(2024, 3, 31, 0, 0)
        date.atTime(endOfDay).toTime() shouldBe MpLocalTime.Min
    }

    "atTime(EndOfDay, zone) is the start of the next day: Berlin 2024-03-30, the night before the switch" {
        val berlin = MpTimezone.of("Europe/Berlin")
        val subject = MpLocalDate.of(2024, 3, 30).atTime(endOfDay, berlin)

        subject shouldBe MpLocalDate.of(2024, 3, 31).atStartOfDay(berlin)
        subject.toInstant() shouldBe MpInstant.parse("2024-03-30T23:00:00Z")
    }

    "atTime(EndOfDay, zone) follows the start-of-day rule when midnight is in a gap (Toronto 1919-03-31: 00:30)" {
        val toronto = MpTimezone.of("America/Toronto")

        val fromDate = MpLocalDate.of(1919, 3, 30).atTime(endOfDay, toronto)
        val fromZoned = MpZonedDateTime.of(MpLocalDateTime.of(1919, 3, 30, 12, 0), toronto).atTime(endOfDay)

        fromDate.datetime shouldBe MpLocalDateTime.of(1919, 3, 31, 0, 30)
        fromDate.toInstant() shouldBe MpInstant.parse("1919-03-31T04:30:00Z")
        fromZoned shouldBe fromDate
        fromDate.toLocalTime() shouldBe MpLocalTime.of(0, 30)
    }

    "MpLocalDate.toRange(slot to EndOfDay, zone) ends at the start of the next day" {
        val berlin = MpTimezone.of("Europe/Berlin")
        val range = MpLocalDate.of(2024, 3, 30).toRange(MpLocalTimeSlot(MpLocalTime.Noon, endOfDay), berlin)

        range.to shouldBe MpLocalDate.of(2024, 3, 31).atStartOfDay(berlin)
    }

    "DateTimeRangeConverter.fromHourToHour(_, 24) ends at the start of the next day" {
        val berlin = MpTimezone.of("Europe/Berlin")
        val date = MpLocalDate.of(2024, 3, 30)

        val range = DateTimeRangeConverter(berlin, date, date).fromHourToHour(fromHour = 8, toHour = 24)

        range.from shouldBe date.atTime(MpLocalTime.of(8, 0), berlin)
        range.to shouldBe MpLocalDate.of(2024, 3, 31).atStartOfDay(berlin)
    }

    "MpLocalTimeSlot up to EndOfDay: valid, 24 h, contains Max, split by 1 h ends with 23:00 to 24:00" {
        val slot = MpLocalTimeSlot(MpLocalTime.Min, endOfDay)
        val split = slot.splitWithGaps(1.hours)

        slot.isValid shouldBe true
        slot.duration shouldBe 24.hours
        slot.contains(MpLocalTime.Max) shouldBe true
        slot.contains(endOfDay) shouldBe false
        split.size shouldBe 24
        split.last() shouldBe MpLocalTimeSlot(MpLocalTime.of(23, 0), endOfDay)
        split.last().formatHhMm() shouldBe "23:00 - 24:00"
        slot.intersects(MpLocalTimeSlot(MpLocalTime.of(23, 0), MpLocalTime.Max)) shouldBe true
    }

    "a slot from Noon split by 5 h with a 1 h gap stops before 24:00 is passed" {
        MpLocalTimeSlot(MpLocalTime.Noon, endOfDay).splitWithGaps(5.hours, 1.hours) shouldBe listOf(
            MpLocalTimeSlot(MpLocalTime.Noon, MpLocalTime.of(17, 0)),
            MpLocalTimeSlot(MpLocalTime.of(18, 0), MpLocalTime.of(23, 0)),
        )
    }

    // ---- ULTRA-00341 round 1 ------------------------------------------------------------------------------------

    "M1: completeDay is no longer [Min, Max); an old stored slot [Min, Max) leaves a 1 ms remainder when cut away" {
        val oldCompleteDay = MpLocalTimeSlot(MpLocalTime.Min, MpLocalTime.Max)

        (oldCompleteDay == MpLocalTimeSlot.completeDay) shouldBe false
        MpLocalTimeSlot.completeDay.cutAway(oldCompleteDay) shouldBe
                listOf(MpLocalTimeSlot(MpLocalTime.Max, MpLocalTime.EndOfDay))
    }

    "M1: completeDay serializes as {\"from\":0,\"to\":86400000} and reads back" {
        val json = Json.encodeToString(MpLocalTimeSlot.serializer(), MpLocalTimeSlot.completeDay)

        json shouldBe """{"from":0,"to":86400000}"""
        Json.decodeFromString(MpLocalTimeSlot.serializer(), json) shouldBe MpLocalTimeSlot.completeDay
    }

    "fix 1: atTime(EndOfDay, zone) on a date the zone skipped is the start of the next day (Apia 2011-12-30)" {
        val apia = MpTimezone.of("Pacific/Apia")
        val date = MpLocalDate.of(2011, 12, 30)

        val subject = date.atTime(endOfDay, apia)

        subject.toInstant() shouldBe MpInstant.parse("2011-12-30T10:00:00Z") // 2011-12-31T00:00+14:00
        subject shouldBe date.plusDays(1).atStartOfDay(apia)
        subject shouldBe date.atTime(endOfDay).atZone(apia)
    }

    "delta round 2: the lower edge of the plus guard: one millisecond already moves EndOfDay" {
        endOfDay.plus(1.milliseconds) shouldBe MpLocalTime.of(0, 0, 0, 1)
        endOfDay.minus(1.milliseconds) shouldBe MpLocalTime.Max
    }

    "delta round 2: ofSecondsOfDay gives EndOfDay only for a from inside the day; otherwise both wrap, as before" {
        MpLocalTimeSlot.ofSecondsOfDay(86_400, 86_400) shouldBe MpLocalTimeSlot(MpLocalTime.Min, MpLocalTime.Min)
        MpLocalTimeSlot.ofSecondsOfDay(90_000, 86_400) shouldBe MpLocalTimeSlot(MpLocalTime.of(1, 0), MpLocalTime.Min)
        MpLocalTimeSlot.ofSecondsOfDay(86_399, 86_400) shouldBe
                MpLocalTimeSlot(MpLocalTime.of(23, 59, 59), endOfDay)
    }

    "delta round 2: of(from, duration) gives EndOfDay only for a from inside the day; otherwise as before" {
        // a raw out-of-range from (the unchecked constructor) whose end lands on one day keeps wrapping
        MpLocalTimeSlot.of(MpLocalTime(-3_600_000), 25.hours).to shouldBe MpLocalTime.Min
        // from = EndOfDay: the end is from.plus(duration), as for any from outside the day
        MpLocalTimeSlot.of(endOfDay, Duration.ZERO) shouldBe MpLocalTimeSlot(endOfDay, endOfDay)
        MpLocalTimeSlot.of(endOfDay, 1.hours) shouldBe MpLocalTimeSlot(endOfDay, MpLocalTime.of(1, 0))
    }

    "fix 2: adding nothing keeps EndOfDay, also below a millisecond; every other value behaves as before" {
        endOfDay.plus(Duration.ZERO) shouldBe endOfDay
        endOfDay.plus(500.microseconds) shouldBe endOfDay
        endOfDay.minus(500.microseconds) shouldBe endOfDay
        MpLocalTime.Noon.plus(500.microseconds) shouldBe MpLocalTime.Noon
        // values from the unchecked constructor are wrapped by plus, also by plus(ZERO), as before
        MpLocalTime(90_000_000).plus(Duration.ZERO) shouldBe MpLocalTime.of(1, 0)
        MpLocalTime(-1).plus(Duration.ZERO) shouldBe MpLocalTime.Max
    }

    "fix 3: fromHourToHour(24, x) wraps the start hour to 00:00 of the first day, as before" {
        val berlin = MpTimezone.of("Europe/Berlin")
        val date = MpLocalDate.of(2024, 3, 30)

        val range = DateTimeRangeConverter(berlin, date, date).fromHourToHour(fromHour = 24, toHour = 12)

        range.from.toInstant() shouldBe MpInstant.parse("2024-03-29T23:00:00Z") // 2024-03-30T00:00+01:00
        range.to.toInstant() shouldBe MpInstant.parse("2024-03-30T11:00:00Z") // 2024-03-30T12:00+01:00
    }

    "fix 5: MpLocalTimeSlot.of / ofSecondsOfDay end at EndOfDay when the end is exactly 24:00; beyond it they wrap" {
        MpLocalTimeSlot.of(MpLocalTime.of(23, 0), 1.hours) shouldBe MpLocalTimeSlot(MpLocalTime.of(23, 0), endOfDay)
        MpLocalTimeSlot.of(MpLocalTime.Min, 24.hours) shouldBe MpLocalTimeSlot.completeDay
        MpLocalTimeSlot.ofSecondsOfDay(0, 86_400) shouldBe MpLocalTimeSlot.completeDay

        MpLocalTimeSlot.of(MpLocalTime.of(23, 0), 2.hours) shouldBe
                MpLocalTimeSlot(MpLocalTime.of(23, 0), MpLocalTime.of(1, 0))
        MpLocalTimeSlot.ofSecondsOfDay(0, 90_000) shouldBe MpLocalTimeSlot(MpLocalTime.Min, MpLocalTime.of(1, 0))
    }

    "fix 6: fromMorningToEvening ends at the start of the next day where midnight is in a gap (Havana, Santiago)" {
        listOf(
            Triple("America/Havana", MpLocalDate.of(2024, 3, 10), "2024-03-11T04:00:00Z"),
            Triple("America/Santiago", MpLocalDate.of(2024, 9, 8), "2024-09-09T03:00:00Z"),
        ).forEach { (zoneId, date, expectedEnd) ->
            val zone = MpTimezone.of(zoneId)
            val converter = DateTimeRangeConverter(zone, date, date)

            converter.fromMorningToEvening.to.toInstant() shouldBe MpInstant.parse(expectedEnd)
            converter.fromMorningToEvening.to shouldBe converter.fromHourToHour(0, 24).to
        }
    }

    "fix 6: fromMorningToEvening on ordinary days and across both Berlin switches is unchanged" {
        val berlin = MpTimezone.of("Europe/Berlin")

        // the start of the next day in Berlin, the same as before the fix
        listOf(
            MpLocalDate.of(2024, 1, 15) to "2024-01-15T23:00:00Z",
            MpLocalDate.of(2024, 3, 30) to "2024-03-30T23:00:00Z",
            MpLocalDate.of(2024, 3, 31) to "2024-03-31T22:00:00Z",
            MpLocalDate.of(2024, 4, 1) to "2024-04-01T22:00:00Z",
            MpLocalDate.of(2024, 10, 26) to "2024-10-26T22:00:00Z",
            MpLocalDate.of(2024, 10, 27) to "2024-10-27T23:00:00Z",
            MpLocalDate.of(2024, 10, 28) to "2024-10-28T23:00:00Z",
        ).forEach { (date, expectedEnd) ->
            val converter = DateTimeRangeConverter(berlin, date, date)

            converter.fromMorningToEvening.to.toInstant() shouldBe MpInstant.parse(expectedEnd)
        }
    }

    "known consequence, pinned: where a gap ends at midnight (Nuuk 2024-03-30) [23:30, 24:00) is an inverted range" {
        val nuuk = MpTimezone.of("America/Nuuk")
        val range = MpLocalDate.of(2024, 3, 30).toRange(MpLocalTimeSlot(MpLocalTime.of(23, 30), endOfDay), nuuk)

        (range.from.toInstant() > range.to.toInstant()) shouldBe true
        range.isValid shouldBe false
    }

    "known consequence, pinned: on a skipped date (Apia 2011-12-30) [09:00, 24:00) is inverted, completeDay empty" {
        val apia = MpTimezone.of("Pacific/Apia")
        val date = MpLocalDate.of(2011, 12, 30)

        val morning = date.toRange(MpLocalTimeSlot(MpLocalTime.of(9, 0), endOfDay), apia)
        val wholeDay = date.toRange(MpLocalTimeSlot.completeDay, apia)

        (morning.from.toInstant() > morning.to.toInstant()) shouldBe true
        morning.isValid shouldBe false
        wholeDay.from.toInstant() shouldBe wholeDay.to.toInstant()
        wholeDay.isValid shouldBe false
    }
})
