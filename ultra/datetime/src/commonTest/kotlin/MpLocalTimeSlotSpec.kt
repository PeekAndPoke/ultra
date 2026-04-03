package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.tuple
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MpLocalTimeSlotSpec : StringSpec({

    "Creation - via constructor" {

        val subject = MpLocalTimeSlot(
            from = MpLocalTime.of(0, 1, 2),
            to = MpLocalTime.of(12, 13, 14)
        )

        subject.from shouldBe MpLocalTime.of(0, 1, 2)
        subject.to shouldBe MpLocalTime.of(12, 13, 14)
    }

    "Creation - via completeDay" {

        val subject = MpLocalTimeSlot.completeDay

        subject.from shouldBe MpLocalTime.Min
        subject.to shouldBe MpLocalTime.Max
    }

    "Creation - via of(from, duration)" {

        val subject = MpLocalTimeSlot.of(
            from = MpLocalTime.of(10, 0),
            duration = 1.hours,
        )

        subject.from shouldBe MpLocalTime.of(10, 0)
        subject.to shouldBe MpLocalTime.of(11, 0)
    }

    "Creation - via ofSecondsOfDay" {
        val subject = MpLocalTimeSlot.ofSecondsOfDay(
            from = 1L * 60 * 60,
            to = 2L * 60 * 60
        )

        subject.from shouldBe MpLocalTime.of(1, 0)
        subject.to shouldBe MpLocalTime.of(2, 0)
    }

    "duration must be correct" {

        val subject = MpLocalTimeSlot(
            from = MpLocalTime.of(1, 0),
            to = MpLocalTime.of(2, 15),
        )

        subject.duration shouldBe 75.minutes

        val subject2 = MpLocalTimeSlot(
            from = MpLocalTime.of(2, 15),
            to = MpLocalTime.of(1, 0),
        )

        subject2.duration shouldBe (-75).minutes
    }

    listOf(
        tuple(
            "Zero duration",
            ts(12, 0, 12, 0),
            false
        ),
        tuple(
            "Negative duration",
            ts(12, 0, 1, 12, 0, 0),
            false
        ),
        tuple(
            "Positive duration",
            ts(12, 0, 0, 12, 0, 1),
            true
        ),
    ).forEach { (title, ts, expected) ->
        "isValid and isNotValid - $title: ${ts.formatHhMmSs()} is ${if (!expected) "NOT " else ""}valid" {
            ts.isValid shouldBe expected
            ts.isNotValid shouldBe !expected
        }
    }

    listOf(
        tuple(
            "Exactly the same",
            ts(10, 0, 11, 0),
            ts(10, 0, 11, 0),
            true,
        ),
        tuple(
            "Fully surrounding",
            ts(10, 0, 11, 0),
            ts(9, 0, 12, 0),
            true,
        ),
        tuple(
            "Within",
            ts(10, 0, 11, 0),
            ts(10, 15, 10, 45),
            true,
        ),
        tuple(
            "Barely touching on the right",
            ts(10, 0, 11, 0),
            ts(11, 0, 12, 0),
            true,
        ),
        tuple(
            "Barely touching on the left",
            ts(10, 0, 11, 0),
            ts(9, 0, 10, 0),
            true,
        ),
        tuple(
            "Too far on the right",
            ts(10, 0, 0, 11, 0, 0),
            ts(11, 0, 1, 12, 0, 0),
            false,
        ),
        tuple(
            "Too far on the left",
            ts(10, 0, 0, 11, 0, 0),
            ts(9, 0, 0, 9, 59, 59),
            false,
        ),
    ).forEach { (title, source, other, expected) ->

        ("touches(other: TimeSlot) - " +
                "$title: ${source.formatHhMmSs()} and ${other.formatHhMmSs()} " +
                "do ${if (expected) "" else "NOT "} touch") {

            source.touches(other) shouldBe expected
        }
    }

    listOf(
        // Touching TimeSlots
        tuple(
            ts(10, 0, 11, 0),
            ts(10, 0, 11, 0),
            listOf(ts(10, 0, 11, 0)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(10, 0, 11, 10),
            listOf(ts(10, 0, 11, 10)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(10, 10, 11, 10),
            listOf(ts(10, 0, 11, 10)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(9, 50, 11, 0),
            listOf(ts(9, 50, 11, 0)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(9, 50, 10, 50),
            listOf(ts(9, 50, 11, 0)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(9, 50, 11, 10),
            listOf(ts(9, 50, 11, 10)),
        ),
        // Not touching TimeSlots
        tuple(
            ts(10, 0, 11, 0),
            ts(8, 0, 9, 0),
            listOf(ts(8, 0, 9, 0), ts(10, 0, 11, 0)),
        ),
        tuple(
            ts(10, 0, 11, 0),
            ts(12, 0, 13, 0),
            listOf(ts(10, 0, 11, 0), ts(12, 0, 13, 0)),
        ),
    ).forEach { (source: MpLocalTimeSlot, other: MpLocalTimeSlot, expected: List<MpLocalTimeSlot>) ->

        ("mergedWith(other: TimeSlot) - " +
                "${source.formatHhMmSs()} merged with ${other.formatHhMmSs()} " +
                "must be ${expected.format()}") {

            source.mergeWith(other) shouldBe expected
        }
    }

    listOf(
        tuple(
            "All eaten up - exactly",
            ts(10, 0, 11, 0),
            ts(10, 0, 11, 0),
            listOf(),
        ),
        tuple(
            "All eaten up - surrounding",
            ts(10, 0, 11, 0),
            ts(9, 59, 11, 1),
            listOf(),
        ),
        tuple(
            "In the middle",
            ts(10, 0, 11, 0),
            ts(10, 1, 10, 59),
            listOf(
                ts(10, 0, 10, 1),
                ts(10, 59, 11, 0),
            ),
        ),
        tuple(
            "In the middle but duration = 0",
            ts(10, 0, 11, 0),
            ts(10, 30, 10, 30),
            listOf(
                ts(10, 0, 11, 0),
            ),
        ),
        tuple(
            "On the left side",
            ts(10, 0, 11, 0),
            ts(10, 0, 10, 1),
            listOf(
                ts(10, 1, 11, 0),
            ),
        ),
        tuple(
            "On the right side",
            ts(10, 0, 11, 0),
            ts(10, 59, 11, 0),
            listOf(
                ts(10, 0, 10, 59),
            ),
        ),
        tuple(
            "Too far on the left side",
            ts(10, 0, 11, 0),
            ts(9, 0, 9, 59),
            listOf(
                ts(10, 0, 11, 0),
            ),
        ),
        tuple(
            "Too far on the right side",
            ts(10, 0, 11, 0),
            ts(11, 1, 12, 0),
            listOf(
                ts(10, 0, 11, 0),
            ),
        ),
    ).forEach { (title, source, other, expected) ->

        ("cutAway(other: TimeSlot) - " +
                "$title: ${source.formatHhMmSs()} cut away ${other.formatHhMmSs()} " +
                "must be ${expected.format()}  ") {

            source.cutAway(other) shouldBe expected
        }
    }

    listOf(
        tuple(
            "Edge case: empty initial slot",
            ts(10, 0, 10, 0),
            10, 0,
            listOf()
        ),
        tuple(
            "Edge case: duration < 1",
            ts(10, 0, 11, 0),
            0, 0,
            listOf()
        ),
        tuple(
            "Edge case: gap < 0",
            ts(10, 0, 11, 0),
            30, -10,
            listOf(
                ts(10, 0, 10, 30),
                ts(10, 30, 11, 0),
            )
        ),
        tuple(
            "Normal case: duration = 15 gap = 5",
            ts(10, 0, 11, 0),
            15, 5,
            listOf(
                ts(10, 0, 10, 15),
                ts(10, 20, 10, 35),
                ts(10, 40, 10, 55),
            )
        ),
        tuple(
            "Normal case: duration = 1 gap = 0",
            ts(10, 0, 10, 5),
            1, 0,
            listOf(
                ts(10, 0, 10, 1),
                ts(10, 1, 10, 2),
                ts(10, 2, 10, 3),
                ts(10, 3, 10, 4),
                ts(10, 4, 10, 5),
            )
        ),
    ).forEach { (title, initial, duration, gap, expected) ->

        ("splitWithGaps() - " +
                "$title: ${initial.formatHhMmSs()} splitByMinutes ($duration, $gap) " +
                "must be ${expected.format()}") {

            initial.splitWithGaps(duration.minutes, gap.minutes) shouldBe expected
        }
    }

    // Partial ////////////////////////////////////////////////////////////////////////////////////////

    "Partial.empty has null from and to" {
        MpLocalTimeSlot.Partial.empty.from shouldBe null
        MpLocalTimeSlot.Partial.empty.to shouldBe null
    }

    "Partial.asValidRange returns valid range" {
        val from = MpLocalTime.of(10, 0)
        val to = MpLocalTime.of(11, 0)

        MpLocalTimeSlot.Partial(from, to).asValidRange() shouldBe MpLocalTimeSlot(from, to)
    }

    "Partial.asValidRange returns null for invalid range" {
        val from = MpLocalTime.of(11, 0)
        val to = MpLocalTime.of(10, 0)

        MpLocalTimeSlot.Partial(from, to).asValidRange() shouldBe null
    }

    "Partial.asValidRange returns null when from or to is null" {
        val time = MpLocalTime.of(10, 0)

        MpLocalTimeSlot.Partial(time, null).asValidRange() shouldBe null
        MpLocalTimeSlot.Partial(null, time).asValidRange() shouldBe null
        MpLocalTimeSlot.Partial.empty.asValidRange() shouldBe null
    }

    // asPartialRange //////////////////////////////////////////////////////////////////////////////////

    "asPartialRange converts to Partial with same from and to" {
        val from = MpLocalTime.of(9, 0)
        val to = MpLocalTime.of(17, 0)
        val slot = MpLocalTimeSlot(from, to)

        val partial = slot.asPartialRange()

        partial.from shouldBe from
        partial.to shouldBe to
    }

    // contains ////////////////////////////////////////////////////////////////////////////////////////

    "contains(time) - within slot" {
        val slot = ts(10, 0, 12, 0)

        slot.contains(MpLocalTime.of(10, 0)) shouldBe true  // from is inclusive
        slot.contains(MpLocalTime.of(11, 0)) shouldBe true
        slot.contains(MpLocalTime.of(11, 59)) shouldBe true
        slot.contains(MpLocalTime.of(12, 0)) shouldBe false // to is exclusive
        slot.contains(MpLocalTime.of(9, 59)) shouldBe false
        slot.contains(MpLocalTime.of(12, 1)) shouldBe false
    }

    "contains(time) - invalid slot returns false" {
        val slot = ts(12, 0, 10, 0)

        slot.contains(MpLocalTime.of(11, 0)) shouldBe false
    }

    "contains(other) - one slot fully contains another" {
        val outer = ts(9, 0, 17, 0)
        val inner = ts(10, 0, 12, 0)

        outer.contains(inner) shouldBe true
        inner.contains(outer) shouldBe false
    }

    "contains(other) - slot contains itself" {
        val slot = ts(10, 0, 12, 0)

        slot.contains(slot) shouldBe true
    }

    "contains(other) - invalid slots return false" {
        val valid = ts(10, 0, 12, 0)
        val invalid = ts(12, 0, 10, 0)

        valid.contains(invalid) shouldBe false
        invalid.contains(valid) shouldBe false
    }

    // intersects //////////////////////////////////////////////////////////////////////////////////////

    "intersects - overlapping slots" {
        val a = ts(10, 0, 12, 0)
        val b = ts(11, 0, 13, 0)

        a.intersects(b) shouldBe true
        b.intersects(a) shouldBe true
    }

    "intersects - non-overlapping slots" {
        val a = ts(10, 0, 11, 0)
        val b = ts(12, 0, 13, 0)

        a.intersects(b) shouldBe false
        b.intersects(a) shouldBe false
    }

    "intersects - adjacent slots do NOT intersect" {
        val a = ts(10, 0, 11, 0)
        val b = ts(11, 0, 12, 0)

        a.intersects(b) shouldBe false
    }

    "intersects - one contains the other" {
        val outer = ts(9, 0, 17, 0)
        val inner = ts(10, 0, 12, 0)

        outer.intersects(inner) shouldBe true
        inner.intersects(outer) shouldBe true
    }

    "intersects - invalid slot returns false" {
        val valid = ts(10, 0, 12, 0)
        val invalid = ts(12, 0, 10, 0)

        valid.intersects(invalid) shouldBe false
    }

    // isAdjacentTo ////////////////////////////////////////////////////////////////////////////////////

    "isAdjacentTo - adjacent slots" {
        val a = ts(10, 0, 11, 0)
        val b = ts(11, 0, 12, 0)

        a.isAdjacentTo(b) shouldBe true
        b.isAdjacentTo(a) shouldBe true
    }

    "isAdjacentTo - overlapping slots are not adjacent" {
        val a = ts(10, 0, 12, 0)
        val b = ts(11, 0, 13, 0)

        a.isAdjacentTo(b) shouldBe false
    }

    "isAdjacentTo - separated slots are not adjacent" {
        val a = ts(10, 0, 11, 0)
        val b = ts(12, 0, 13, 0)

        a.isAdjacentTo(b) shouldBe false
    }

    // formatHhMm / formatHhMmSs ///////////////////////////////////////////////////////////////////////

    "formatHhMm" {
        val subject = MpLocalTimeSlot(
            from = MpLocalTime.of(10, 15),
            to = MpLocalTime.of(12, 30),
        )

        subject.formatHhMm() shouldBe "10:15 - 12:30"
    }

    "formatHhMmSs" {
        val subject = MpLocalTimeSlot(
            from = MpLocalTime.of(10, 15, 30),
            to = MpLocalTime.of(12, 30, 45),
        )

        subject.formatHhMmSs() shouldBe "10:15:30 - 12:30:45"
    }
})
