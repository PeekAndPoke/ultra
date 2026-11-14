package io.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** ULTRA-00341 batch 3b-2: on a date a slot ends by the clock, which respects DST. */
@Suppress("unused")
class MpLocalTimeSlotOnDateSpec : StringSpec({

    val berlin = MpTimezone.of("Europe/Berlin")

    fun t(hour: Int, minute: Int = 0) = MpLocalTime.of(hour, minute)
    fun slot(hour: Int, minute: Int, duration: Duration) = MpLocalTimeSlot.of(t(hour, minute), duration)
    fun i(iso: String) = MpInstant.parse(iso)
    fun MpZonedDateTimeRange.instants() = from.toInstant() to to.toInstant()

    "an ordinary night: 22:00 to 02:00 the next day" {
        MpLocalDate.of(2024, 1, 15).toRange(slot(22, 0, 4.hours), berlin).instants() shouldBe
                (i("2024-01-15T21:00:00Z") to i("2024-01-16T01:00:00Z"))
    }

    "by the clock on DST nights: 22:00 + 8 h ends at 06:00, which is 7 h in spring and 9 h in autumn" {
        val night = slot(22, 0, 8.hours)

        val spring = MpLocalDate.of(2024, 3, 30).toRange(night, berlin)
        spring.instants() shouldBe (i("2024-03-30T21:00:00Z") to i("2024-03-31T04:00:00Z"))
        spring.duration shouldBe 7.hours

        val autumn = MpLocalDate.of(2024, 10, 26).toRange(night, berlin)
        autumn.instants() shouldBe (i("2024-10-26T20:00:00Z") to i("2024-10-27T05:00:00Z"))
        autumn.duration shouldBe 9.hours
    }

    "an end at 24:00 is the start of the next day; completeDay on a 23-hour day lasts 23 hours" {
        MpLocalDate.of(2024, 1, 15).toRange(slot(23, 0, 1.hours), berlin).instants() shouldBe
                (i("2024-01-15T22:00:00Z") to i("2024-01-15T23:00:00Z"))
        MpLocalDate.of(2024, 3, 31).toRange(MpLocalTimeSlot.completeDay, berlin).duration shouldBe 23.hours
    }

    "longer than a day: 10:00 + 48 h ends at 10:00 two days later" {
        MpLocalDate.of(2024, 1, 15).toRange(slot(10, 0, 48.hours), berlin).instants() shouldBe
                (i("2024-01-15T09:00:00Z") to i("2024-01-17T09:00:00Z"))
    }

    "a start or end in a skipped hour moves to the first existing time, the transition (Berlin 2024-03-31)" {
        // 02:00 to 03:00 is missing; the transition is 01:00Z (03:00+02:00)
        val date = MpLocalDate.of(2024, 3, 31)

        date.toRange(slot(2, 30, 1.hours), berlin).instants() shouldBe
                (i("2024-03-31T01:00:00Z") to i("2024-03-31T01:30:00Z"))
        date.toRange(slot(2, 30, 2.hours), berlin).instants() shouldBe
                (i("2024-03-31T01:00:00Z") to i("2024-03-31T02:30:00Z"))
        date.toRange(slot(1, 0, 90.minutes), berlin).instants() shouldBe
                (i("2024-03-31T00:00:00Z") to i("2024-03-31T01:00:00Z"))
        date.toRange(slot(1, 0, 2.hours), berlin).instants() shouldBe
                (i("2024-03-31T00:00:00Z") to i("2024-03-31T01:00:00Z"))
        date.toRange(slot(2, 0, 1.hours), berlin).instants() shouldBe
                (i("2024-03-31T01:00:00Z") to i("2024-03-31T01:00:00Z"))
        date.toRange(slot(2, 15, 30.minutes), berlin).instants() shouldBe
                (i("2024-03-31T01:00:00Z") to i("2024-03-31T01:00:00Z"))
    }

    "on a DST night the parts of a split add up to the whole slot's real length" {
        val date = MpLocalDate.of(2024, 3, 31)
        val whole = slot(1, 0, 3.hours)

        val parts = whole.splitWithGaps(30.minutes).map { date.toRange(it, berlin).duration }

        parts shouldBe listOf(30.minutes, 30.minutes, Duration.ZERO, Duration.ZERO, 30.minutes, 30.minutes)
        parts.reduce { a, b -> a + b } shouldBe date.toRange(whole, berlin).duration
        date.toRange(whole, berlin).duration shouldBe 2.hours
    }

    "a repeated hour takes its first occurrence (Berlin 2024-10-27, 02:00 to 03:00 twice)" {
        MpLocalDate.of(2024, 10, 27).toRange(slot(1, 30, 1.hours), berlin).instants() shouldBe
                (i("2024-10-26T23:30:00Z") to i("2024-10-27T00:30:00Z"))
        MpLocalDate.of(2024, 10, 26).toRange(slot(22, 0, 270.minutes), berlin).instants() shouldBe
                (i("2024-10-26T20:00:00Z") to i("2024-10-27T00:30:00Z"))
    }

    "a gap that ends at midnight (Nuuk 2024-03-30, 23:00 to 24:00 missing): [23:30, 24:00) is empty at the transition" {
        val nuuk = MpLocalDate.of(2024, 3, 30).toRange(slot(23, 30, 30.minutes), MpTimezone.of("America/Nuuk"))

        nuuk.instants() shouldBe (i("2024-03-31T01:00:00Z") to i("2024-03-31T01:00:00Z"))
    }

    "on a skipped date (Apia 2011-12-30) every slot ending that day is empty at the next day's start" {
        val apia = MpTimezone.of("Pacific/Apia")
        val skipped = MpLocalDate.of(2011, 12, 30)
        val nextDayStart = i("2011-12-30T10:00:00Z") // 2011-12-31T00:00+14:00

        skipped.toRange(slot(9, 0, 8.hours), apia).instants() shouldBe (nextDayStart to nextDayStart)
        skipped.toRange(slot(9, 0, 899.minutes), apia).instants() shouldBe (nextDayStart to nextDayStart)
        skipped.toRange(MpLocalTimeSlot.completeDay, apia).instants() shouldBe (nextDayStart to nextDayStart)
        // a slot running into the next day keeps its part there
        skipped.toRange(slot(9, 0, 16.hours), apia).instants() shouldBe (nextDayStart to i("2011-12-30T11:00:00Z"))
    }

    "an end beyond what the platform resolves stops at its last representable instant (JS: 8.64e15 ms)" {
        val far = slot(10, 0, 8_700_000_000_000_000L.milliseconds)

        // UTC has no zone-engine limit on any platform: the exact end, start + length
        MpLocalDate.of(2024, 1, 15).toRange(far, MpTimezone.UTC).instants() shouldBe
                (i("2024-01-15T10:00:00Z") to MpInstant.fromEpochMillis(8_701_705_312_800_000L))

        // a region zone: JS stops at the end of its Date range, JVM and native resolve the end
        val range = MpLocalDate.of(2024, 1, 15).toRange(far, berlin)
        range.from.toInstant() shouldBe i("2024-01-15T09:00:00Z")
        when (val limit = zoneEngineLimitMillis) {
            null -> (range.to.toInstant() > MpInstant.fromEpochMillis(8_640_000_000_000_000L)) shouldBe true
            else -> range.to.toInstant() shouldBe MpInstant.fromEpochMillis(limit)
        }
    }

    "a start the platform cannot represent throws, naming the date, the zone and the limit (JS only)" {
        val date = MpLocalDate.of(300_000, 1, 1)

        when (zoneEngineLimitMillis) {
            null -> date.toRange(slot(10, 0, 1.hours), berlin).duration shouldBe 1.hours
            else -> {
                val error = shouldThrow<IllegalArgumentException> { date.toRange(slot(10, 0, 1.hours), berlin) }

                error.message shouldContain "300000-01-01"
                error.message shouldContain "Europe/Berlin"
                error.message shouldContain "275760-09-13"
            }
        }
        // UTC resolves everywhere
        date.toRange(slot(10, 0, 1.hours), MpTimezone.UTC).duration shouldBe 1.hours
    }

    "a fixed offset has no gaps: 02:30 + 1 h at +02:00 is just 00:30Z to 01:30Z" {
        MpLocalDate.of(2024, 3, 31).toRange(slot(2, 30, 1.hours), MpTimezone.of("+02:00")).instants() shouldBe
                (i("2024-03-31T00:30:00Z") to i("2024-03-31T01:30:00Z"))
    }

    "an end past the last date the platform holds stops at that date's last instant (999 999 999-12-31)" {
        val last = MpLocalDate.of(999_999_999, 12, 31)
        val range = last.toRange(slot(22, 0, 4.hours), MpTimezone.UTC)

        range.from.toInstant() shouldBe last.atTime(t(22), MpTimezone.UTC).toInstant()
        // 999 999 999-12-31T22:00Z, by days-from-civil (checked independently, 2026-11-14)
        range.from.toInstant().value.epochSeconds shouldBe 31_556_889_832_773_600L
        // 999 999 999-12-31T23:59:59.999Z, the last instant of the last date
        range.to.toInstant().value.epochSeconds shouldBe 31_556_889_832_780_799L
        range.to.toInstant().value.nanosecondsOfSecond shouldBe 999_000_000
        // an end at 24:00 of the last date falls back the same way
        last.toRange(slot(22, 0, 2.hours), MpTimezone.UTC).to shouldBe range.to
    }

    "an empty slot gives an empty range at its start" {
        val range = MpLocalDate.of(2024, 1, 15).toRange(slot(10, 0, Duration.ZERO), berlin)

        range.instants() shouldBe (i("2024-01-15T09:00:00Z") to i("2024-01-15T09:00:00Z"))
    }

    "Duration.INFINITE on a date: the longest slot, a two-ended range" {
        val range = MpLocalDate.of(2024, 1, 15).toRange(slot(10, 0, Duration.INFINITE), MpTimezone.UTC)

        // 2024-01-15T10:00Z + (2^53 - 1) ms, by hand
        range.instants() shouldBe (i("2024-01-15T10:00:00Z") to MpInstant.fromEpochMillis(9_008_904_567_540_991L))
    }

    "the TimeZone overload gives the same range (UTC: kotlinx on JS has no zone database in these tests)" {
        val date = MpLocalDate.of(2024, 1, 15)

        date.toRange(slot(22, 0, 4.hours), TimeZone.UTC) shouldBe date.toRange(slot(22, 0, 4.hours), MpTimezone.UTC)
    }
})
