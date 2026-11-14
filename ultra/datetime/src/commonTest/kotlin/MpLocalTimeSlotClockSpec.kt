package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.ComputedList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** ULTRA-00341 batch 3b-2: a slot is a start and a duration; without a date it is a pattern on the 24-hour clock. */
@Suppress("unused")
class MpLocalTimeSlotClockSpec : StringSpec({

    fun t(hour: Int, minute: Int = 0) = MpLocalTime.of(hour, minute)
    fun slot(hour: Int, minute: Int, duration: Duration) = MpLocalTimeSlot.of(t(hour, minute), duration)

    val night = slot(22, 0, 4.hours) // 22:00 to 02:00

    // construction and the derived end ////////////////////////////////////////////////////////////////////////////

    "a start and a duration; to, endDayOffset and crossesMidnight are derived" {
        night.from shouldBe t(22)
        night.duration shouldBe 4.hours
        night.to shouldBe t(2)
        night.endDayOffset shouldBe 1L
        night.crossesMidnight shouldBe true

        slot(23, 0, 1.hours).let { listOf(it.to, it.endDayOffset, it.crossesMidnight) } shouldBe
                listOf(MpLocalTime.EndOfDay, 0L, false)
        slot(0, 0, 30.hours).let { listOf(it.to, it.endDayOffset) } shouldBe listOf(t(6), 1L)
        slot(10, 0, 48.hours).let { listOf(it.to, it.endDayOffset) } shouldBe listOf(t(10), 2L)
        slot(0, 0, 48.hours).let { listOf(it.to, it.endDayOffset) } shouldBe listOf(MpLocalTime.EndOfDay, 1L)
        slot(10, 0, 2.hours).let { listOf(it.to, it.endDayOffset, it.crossesMidnight) } shouldBe
                listOf(t(12), 0L, false)
    }

    "completeDay is 00:00 for 24 hours, ending at 24:00" {
        MpLocalTimeSlot.completeDay shouldBe MpLocalTimeSlot.of(MpLocalTime.Min, 24.hours)
        MpLocalTimeSlot.completeDay.to shouldBe MpLocalTime.EndOfDay
        MpLocalTimeSlot.completeDay.endDayOffset shouldBe 0L
    }

    "an empty slot: zero or negative duration, its end is its start" {
        val empty = slot(10, 0, Duration.ZERO)

        empty.isEmpty shouldBe true
        empty.isValid shouldBe false
        empty.to shouldBe t(10)
        empty.endDayOffset shouldBe 0L
        slot(10, 0, (-1).hours) shouldBe empty
        night.isEmpty shouldBe false
        night.isValid shouldBe true
    }

    "durations are cut to whole milliseconds; the start wraps into the day, 24:00 is 00:00" {
        slot(10, 0, 1500.microseconds).duration shouldBe 1.milliseconds
        slot(10, 0, 999.microseconds).isEmpty shouldBe true
        MpLocalTimeSlot.of(MpLocalTime.EndOfDay, 1.hours) shouldBe slot(0, 0, 1.hours)
        MpLocalTimeSlot.of(MpLocalTime(-3_600_000), 1.hours) shouldBe slot(23, 0, 1.hours)
    }

    "Duration.INFINITE is kept; a duration from 2^53 - 1 ms on is infinite" {
        val open = slot(10, 0, Duration.INFINITE)

        open.duration shouldBe Duration.INFINITE
        open.isInfinite shouldBe true
        open.to shouldBe t(10)
        open.endDayOffset shouldBe Long.MAX_VALUE
        slot(10, 0, MpLocalTimeSlot.INFINITE_DURATION_MILLIS.milliseconds) shouldBe open
        slot(10, 0, (MpLocalTimeSlot.INFINITE_DURATION_MILLIS - 1).milliseconds).isInfinite shouldBe false
    }

    "of(from, to): to after from is the difference, before from is overnight, equal is empty" {
        MpLocalTimeSlot.of(t(22), t(2)) shouldBe night
        MpLocalTimeSlot.of(t(10), t(12)) shouldBe slot(10, 0, 2.hours)
        MpLocalTimeSlot.of(t(10), t(10)).isEmpty shouldBe true
        // equal as given: 24:00 to 24:00 is empty too, not a whole day
        MpLocalTimeSlot.of(MpLocalTime.EndOfDay, MpLocalTime.EndOfDay).isEmpty shouldBe true
        MpLocalTimeSlot.of(t(0), MpLocalTime.EndOfDay) shouldBe MpLocalTimeSlot.completeDay
        MpLocalTimeSlot.of(t(23), MpLocalTime.EndOfDay) shouldBe slot(23, 0, 1.hours)
    }

    "ofSecondsOfDay: the same rules, 86 400 is 24:00, equal values are empty" {
        MpLocalTimeSlot.ofSecondsOfDay(79_200, 7_200) shouldBe night
        MpLocalTimeSlot.ofSecondsOfDay(0, 86_400) shouldBe MpLocalTimeSlot.completeDay
        MpLocalTimeSlot.ofSecondsOfDay(86_400, 86_400).isEmpty shouldBe true
        MpLocalTimeSlot.ofSecondsOfDay(90_000, 86_400) shouldBe slot(1, 0, 23.hours)
    }

    "Partial is the two-picker form helper: from and to, an empty slot is not valid" {
        MpLocalTimeSlot.Partial(t(22), t(2)).asValidRange() shouldBe night
        MpLocalTimeSlot.Partial(t(10), t(10)).asValidRange() shouldBe null
        MpLocalTimeSlot.Partial(MpLocalTime.EndOfDay, MpLocalTime.EndOfDay).asValidRange() shouldBe null
        MpLocalTimeSlot.Partial(null, t(10)).asValidRange() shouldBe null
        night.asPartialRange() shouldBe MpLocalTimeSlot.Partial(t(22), t(2))
    }

    "formatting shows from and the derived to" {
        night.formatHhMm() shouldBe "22:00 - 02:00"
        MpLocalTimeSlot.completeDay.formatHhMm() shouldBe "00:00 - 24:00"
    }

    // predicates on the clock face ////////////////////////////////////////////////////////////////////////////////

    "contains(time): on the clock face; 24:00 counts as 00:00; an empty slot contains nothing" {
        listOf(t(22), t(23), t(0), t(1), MpLocalTime.EndOfDay).map { night.contains(it) } shouldBe List(5) { true }
        listOf(t(2), t(21, 59), t(12)).map { night.contains(it) } shouldBe List(3) { false }
        slot(10, 0, 2.hours).contains(MpLocalTime.EndOfDay) shouldBe false
        MpLocalTimeSlot.completeDay.contains(MpLocalTime.EndOfDay) shouldBe true
        slot(5, 0, 30.hours).contains(t(4)) shouldBe true
        slot(10, 0, Duration.ZERO).contains(t(10)) shouldBe false
    }

    "contains(slot): every arc of the other lies in this one" {
        night.contains(slot(23, 0, 2.hours)) shouldBe true
        night.contains(slot(21, 0, 2.hours)) shouldBe false
        night.contains(night) shouldBe true
        MpLocalTimeSlot.completeDay.contains(night) shouldBe true
        night.contains(MpLocalTimeSlot.completeDay) shouldBe false
        night.contains(slot(23, 0, Duration.ZERO)) shouldBe false
        slot(23, 0, Duration.ZERO).contains(slot(23, 0, Duration.ZERO)) shouldBe false
    }

    "intersects: a shared positive length; adjacent slots do not intersect" {
        night.intersects(slot(1, 0, 2.hours)) shouldBe true
        night.intersects(slot(2, 0, 1.hours)) shouldBe false
        night.intersects(slot(12, 0, 1.hours)) shouldBe false
        slot(23, 0, 2.hours).intersects(slot(21, 0, 4.hours)) shouldBe true // two wrapping slots
        MpLocalTimeSlot.completeDay.intersects(slot(3, 0, 1.hours)) shouldBe true
        slot(5, 0, 30.hours).intersects(slot(3, 0, 1.hours)) shouldBe true
        night.intersects(slot(23, 0, Duration.ZERO)) shouldBe false
        MpLocalTimeSlot.completeDay.intersects(slot(3, 0, Duration.ZERO)) shouldBe false
    }

    "touches: the union is contiguous on the clock, also across midnight; an empty slot by its point" {
        night.touches(slot(2, 0, 1.hours)) shouldBe true
        night.touches(slot(3, 0, 1.hours)) shouldBe false
        slot(23, 0, 1.hours).touches(slot(0, 0, 1.hours)) shouldBe true
        slot(0, 0, 1.hours).touches(slot(23, 0, 1.hours)) shouldBe true
        slot(10, 0, 1.hours).touches(slot(12, 0, 1.hours)) shouldBe false
        night.touches(slot(23, 0, Duration.ZERO)) shouldBe true
        night.touches(slot(3, 0, Duration.ZERO)) shouldBe false
        slot(0, 0, Duration.ZERO).touches(slot(23, 0, 1.hours)) shouldBe true
    }

    "isAdjacentTo: one ends where the other begins, without overlap; full and empty slots are adjacent to nothing" {
        night.isAdjacentTo(slot(2, 0, 1.hours)) shouldBe true
        slot(2, 0, 1.hours).isAdjacentTo(night) shouldBe true
        slot(23, 0, 1.hours).isAdjacentTo(slot(0, 0, 1.hours)) shouldBe true
        night.isAdjacentTo(slot(1, 0, 2.hours)) shouldBe false
        MpLocalTimeSlot.completeDay.isAdjacentTo(slot(0, 0, 1.hours)) shouldBe false
        night.isAdjacentTo(slot(2, 0, Duration.ZERO)) shouldBe false
        slot(2, 0, Duration.ZERO).isAdjacentTo(night) shouldBe false
        // the ends meet, but the slots overlap elsewhere on the clock
        slot(10, 0, 20.hours).isAdjacentTo(slot(6, 0, 10.hours)) shouldBe false
        slot(10, 0, 2.hours).isAdjacentTo(slot(12, 0, 23.hours)) shouldBe false
    }

    "mergeWith: touching slots give the union arc; apart they stay sorted by start; a whole clock is 24 hours" {
        night.mergeWith(slot(2, 0, 1.hours)) shouldBe listOf(slot(22, 0, 5.hours))
        slot(2, 0, 1.hours).mergeWith(night) shouldBe listOf(slot(22, 0, 5.hours))
        night.mergeWith(slot(12, 0, 1.hours)) shouldBe listOf(slot(12, 0, 1.hours), night)
        slot(23, 0, 2.hours).mergeWith(slot(0, 30, 2.hours)) shouldBe listOf(slot(23, 0, 210.minutes))
        slot(0, 30, 2.hours).mergeWith(slot(23, 0, 2.hours)) shouldBe listOf(slot(23, 0, 210.minutes))
        slot(20, 0, 8.hours).mergeWith(slot(2, 0, 20.hours)) shouldBe listOf(slot(20, 0, 24.hours))
        slot(5, 0, 30.hours).mergeWith(slot(10, 0, 1.hours)) shouldBe listOf(slot(5, 0, 30.hours))
        slot(10, 0, 1.hours).mergeWith(slot(5, 0, 30.hours)) shouldBe listOf(slot(5, 0, 30.hours))
        // two whole-clock slots of equal length: this one stays
        slot(5, 0, 24.hours).mergeWith(slot(7, 0, 24.hours)) shouldBe listOf(slot(5, 0, 24.hours))
        night.mergeWith(slot(23, 0, Duration.ZERO)) shouldBe listOf(night)
    }

    "cutAway: in this slot's own frame, from its start; the length beyond 24 hours is dropped" {
        night.cutAway(slot(23, 0, 2.hours)) shouldBe listOf(slot(22, 0, 1.hours), slot(1, 0, 1.hours))
        MpLocalTimeSlot.completeDay.cutAway(slot(10, 0, 1.hours)) shouldBe
                listOf(slot(0, 0, 10.hours), slot(11, 0, 13.hours))
        night.cutAway(slot(12, 0, 1.hours)) shouldBe listOf(night)
        night.cutAway(MpLocalTimeSlot.completeDay) shouldBe emptyList()
        night.cutAway(slot(10, 0, Duration.INFINITE)) shouldBe emptyList()
        night.cutAway(slot(1, 0, 3.hours)) shouldBe listOf(slot(22, 0, 3.hours))
        night.cutAway(slot(21, 0, 2.hours)) shouldBe listOf(slot(23, 0, 3.hours))
        slot(5, 0, 30.hours).cutAway(slot(10, 0, 1.hours)) shouldBe listOf(slot(5, 0, 5.hours), slot(11, 0, 18.hours))
        // frame 05:00 to 05:00 the next day: 03:00 to 04:00 lies at 22 to 23 hours
        slot(5, 0, 30.hours).cutAway(slot(3, 0, 1.hours)) shouldBe listOf(slot(5, 0, 22.hours), slot(4, 0, 1.hours))
        slot(10, 0, Duration.ZERO).cutAway(night) shouldBe listOf(slot(10, 0, Duration.ZERO))
        night.cutAway(slot(23, 0, Duration.ZERO)) shouldBe listOf(night)
    }

    // splitWithGaps ///////////////////////////////////////////////////////////////////////////////////////////////

    "splitWithGaps walks the slot's own length from its start: overnight, with a gap, longer than a day" {
        val split = night.splitWithGaps(1.hours)

        (split is ComputedList<*>) shouldBe true
        split shouldBe listOf(slot(22, 0, 1.hours), slot(23, 0, 1.hours), slot(0, 0, 1.hours), slot(1, 0, 1.hours))
        night.splitWithGaps(1.hours, 30.minutes) shouldBe
                listOf(slot(22, 0, 1.hours), slot(23, 30, 1.hours), slot(1, 0, 1.hours))
        slot(0, 0, 48.hours).splitWithGaps(12.hours) shouldBe
                listOf(slot(0, 0, 12.hours), slot(12, 0, 12.hours), slot(0, 0, 12.hours), slot(12, 0, 12.hours))
        MpLocalTimeSlot.completeDay.splitWithGaps(1.hours).let { listOf(it.size, it.last().to) } shouldBe
                listOf(24, MpLocalTime.EndOfDay)
    }

    "splitWithGaps: an empty slot or a part of zero or below gives nothing; a negative gap counts as zero" {
        slot(10, 0, Duration.ZERO).splitWithGaps(1.hours) shouldBe emptyList()
        night.splitWithGaps(Duration.ZERO) shouldBe emptyList()
        night.splitWithGaps((-1).hours) shouldBe emptyList()
        night.splitWithGaps(5.hours) shouldBe emptyList()
        night.splitWithGaps(2.hours, (-1).hours) shouldBe listOf(slot(22, 0, 2.hours), slot(0, 0, 2.hours))
    }

    "splitWithGaps never throws and is free whatever the count: a whole day by 1 ms, an infinite slot by 1 h" {
        MpLocalTimeSlot.completeDay.splitWithGaps(1.milliseconds).size shouldBe 86_400_000

        val endless = slot(10, 0, Duration.INFINITE).splitWithGaps(1.hours)

        endless.size shouldBe Int.MAX_VALUE // capped: a List cannot be larger
        endless[Int.MAX_VALUE - 1] shouldBe slot(16, 0, 1.hours) // (10 + 2 147 483 646) mod 24 = 16
    }

    "splitWithGaps on an infinite slot: endless parts whatever the step; an infinite gap or part gives one part" {
        val open = slot(10, 0, Duration.INFINITE)

        open.splitWithGaps(60.days).size shouldBe Int.MAX_VALUE
        // 60 days is whole days, so every part starts at 10:00; without the step taken mod day, i * step overflows
        open.splitWithGaps(60.days)[Int.MAX_VALUE - 1] shouldBe slot(10, 0, 60.days)
        open.splitWithGaps(1.hours, 60.days).size shouldBe Int.MAX_VALUE
        open.splitWithGaps(1.hours, Duration.INFINITE) shouldBe listOf(slot(10, 0, 1.hours))
        open.splitWithGaps(Duration.INFINITE) shouldBe listOf(open)
        night.splitWithGaps(Duration.INFINITE) shouldBe emptyList()
        night.splitWithGaps(1.hours, Duration.INFINITE) shouldBe listOf(slot(22, 0, 1.hours))
    }
})
