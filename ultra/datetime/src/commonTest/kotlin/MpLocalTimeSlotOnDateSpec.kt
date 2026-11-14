package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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

    "a start or end in a skipped hour moves forward (Berlin 2024-03-31, 02:00 to 03:00 is missing)" {
        val date = MpLocalDate.of(2024, 3, 31)

        date.toRange(slot(2, 30, 2.hours), berlin).instants() shouldBe
                (i("2024-03-31T01:30:00Z") to i("2024-03-31T02:30:00Z"))
        date.toRange(slot(1, 0, 90.minutes), berlin).instants() shouldBe
                (i("2024-03-31T00:00:00Z") to i("2024-03-31T01:30:00Z"))
    }

    "a start moved past the end gives an empty range (it was inverted before)" {
        // Nuuk 2024-03-30: 23:00 to 24:00 is missing, so 23:30 moves to 00:30, past the end at 24:00
        val nuuk = MpLocalDate.of(2024, 3, 30).toRange(slot(23, 30, 30.minutes), MpTimezone.of("America/Nuuk"))
        nuuk.duration shouldBe Duration.ZERO
        nuuk.from shouldBe nuuk.to

        // Apia skipped 2011-12-30 entirely
        val apia = MpTimezone.of("Pacific/Apia")
        val skipped = MpLocalDate.of(2011, 12, 30)
        skipped.toRange(slot(9, 0, 15.hours), apia).duration shouldBe Duration.ZERO
        skipped.toRange(MpLocalTimeSlot.completeDay, apia).duration shouldBe Duration.ZERO
    }

    "an empty slot gives an empty range at its start" {
        val range = MpLocalDate.of(2024, 1, 15).toRange(slot(10, 0, Duration.ZERO), berlin)

        range.instants() shouldBe (i("2024-01-15T09:00:00Z") to i("2024-01-15T09:00:00Z"))
    }

    "an infinite slot gives a range with an open end" {
        val range = MpLocalDate.of(2024, 1, 15).toRange(slot(10, 0, Duration.INFINITE), berlin)

        range.from.toInstant() shouldBe i("2024-01-15T09:00:00Z")
        range.hasEnd shouldBe false
        range.hasStart shouldBe true
    }

    "the TimeZone overload gives the same range (UTC: kotlinx on JS has no zone database in these tests)" {
        val date = MpLocalDate.of(2024, 1, 15)

        date.toRange(slot(22, 0, 4.hours), TimeZone.UTC) shouldBe date.toRange(slot(22, 0, 4.hours), MpTimezone.UTC)
    }
})
