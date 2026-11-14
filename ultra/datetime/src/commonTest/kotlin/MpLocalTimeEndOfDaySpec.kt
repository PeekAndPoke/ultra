package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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
})
