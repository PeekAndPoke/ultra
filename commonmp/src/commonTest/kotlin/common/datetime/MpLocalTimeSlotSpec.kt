package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

    "Creation - via ofSecondsOfDay" {
        val subject = MpLocalTimeSlot.ofSecondsOfDay(
            from = 1L * 60 * 60,
            to = 2L * 60 * 60
        )

        subject.from shouldBe MpLocalTime.of(1, 0)
        subject.to shouldBe MpLocalTime.of(2, 0)
    }

//    "duration must be correct" {
//
//        val subject = TimeSlot(
//            from = LocalTime.of(1, 0),
//            to = LocalTime.of(2, 15),
//        )
//
//        subject.duration shouldBe Duration.ofMinutes(75)
//
//        val subject2 = TimeSlot(
//            from = LocalTime.of(2, 15),
//            to = LocalTime.of(1, 0),
//        )
//
//        subject2.duration shouldBe Duration.ofMinutes(75).negated()
//    }
//
//    "isValid and isNotValid" - {
//
//        listOf(
//            row(
//                "Zero duration",
//                ts(12, 0, 12, 0),
//                false
//            ),
//            row(
//                "Negative duration",
//                ts(12, 0, 1, 12, 0, 0),
//                false
//            ),
//            row(
//                "Positive duration",
//                ts(12, 0, 0, 12, 0, 1),
//                true
//            ),
//        ).forEach { (title, ts, expected) ->
//
//            "$title: ${ts.format()} is ${if (!expected) "NOT " else ""}valid" {
//
//                ts.isValid shouldBe expected
//                ts.isNotValid shouldBe !expected
//            }
//        }
//    }
//
//    "touches(other: TimeSlot)" - {
//
//        listOf(
//            row(
//                "Exactly the same",
//                ts(10, 0, 11, 0),
//                ts(10, 0, 11, 0),
//                true,
//            ),
//            row(
//                "Fully surrounding",
//                ts(10, 0, 11, 0),
//                ts(9, 0, 12, 0),
//                true,
//            ),
//            row(
//                "Within",
//                ts(10, 0, 11, 0),
//                ts(10, 15, 10, 45),
//                true,
//            ),
//            row(
//                "Barely touching on the right",
//                ts(10, 0, 11, 0),
//                ts(11, 0, 12, 0),
//                true,
//            ),
//            row(
//                "Barely touching on the left",
//                ts(10, 0, 11, 0),
//                ts(9, 0, 10, 0),
//                true,
//            ),
//            row(
//                "Too far on the right",
//                ts(10, 0, 0, 11, 0, 0),
//                ts(11, 0, 1, 12, 0, 0),
//                false,
//            ),
//            row(
//                "Too far on the left",
//                ts(10, 0, 0, 11, 0, 0),
//                ts(9, 0, 0, 9, 59, 59),
//                false,
//            ),
//        ).forEach { (title, source, other, expected) ->
//
//            "$title: ${source.format()} and ${other.format()} do ${if (expected) "" else "NOT "} touch" {
//
//                source.touches(other) shouldBe expected
//            }
//        }
//    }
//
//    "mergedWith(other: TimeSlot)" - {
//
//        listOf(
//            // Touching TimeSlots
//            row(
//                ts(10, 0, 11, 0),
//                ts(10, 0, 11, 0),
//                listOf(ts(10, 0, 11, 0)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(10, 0, 11, 10),
//                listOf(ts(10, 0, 11, 10)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(10, 10, 11, 10),
//                listOf(ts(10, 0, 11, 10)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(9, 50, 11, 0),
//                listOf(ts(9, 50, 11, 0)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(9, 50, 10, 50),
//                listOf(ts(9, 50, 11, 0)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(9, 50, 11, 10),
//                listOf(ts(9, 50, 11, 10)),
//            ),
//            // Not touching TimeSlots
//            row(
//                ts(10, 0, 11, 0),
//                ts(8, 0, 9, 0),
//                listOf(ts(8, 0, 9, 0), ts(10, 0, 11, 0)),
//            ),
//            row(
//                ts(10, 0, 11, 0),
//                ts(12, 0, 13, 0),
//                listOf(ts(10, 0, 11, 0), ts(12, 0, 13, 0)),
//            ),
//        ).forEach { (source: TimeSlot, other: TimeSlot, expected: List<TimeSlot>) ->
//
//            "${source.format()} merged with ${other.format()} must be ${expected.format()}" {
//
//                source.mergeWith(other) shouldBe expected
//            }
//        }
//    }
//
//    "cutAway(other: TimeSlot)" - {
//
//        listOf(
//            row(
//                "All eaten up - exactly",
//                ts(10, 0, 11, 0),
//                ts(10, 0, 11, 0),
//                listOf(),
//            ),
//            row(
//                "All eaten up - surrounding",
//                ts(10, 0, 11, 0),
//                ts(9, 59, 11, 1),
//                listOf(),
//            ),
//            row(
//                "In the middle",
//                ts(10, 0, 11, 0),
//                ts(10, 1, 10, 59),
//                listOf(
//                    ts(10, 0, 10, 1),
//                    ts(10, 59, 11, 0),
//                ),
//            ),
//            row(
//                "In the middle but duration = 0",
//                ts(10, 0, 11, 0),
//                ts(10, 30, 10, 30),
//                listOf(
//                    ts(10, 0, 11, 0),
//                ),
//            ),
//            row(
//                "On the left side",
//                ts(10, 0, 11, 0),
//                ts(10, 0, 10, 1),
//                listOf(
//                    ts(10, 1, 11, 0),
//                ),
//            ),
//            row(
//                "On the right side",
//                ts(10, 0, 11, 0),
//                ts(10, 59, 11, 0),
//                listOf(
//                    ts(10, 0, 10, 59),
//                ),
//            ),
//            row(
//                "Too far on the left side",
//                ts(10, 0, 11, 0),
//                ts(9, 0, 9, 59),
//                listOf(
//                    ts(10, 0, 11, 0),
//                ),
//            ),
//            row(
//                "Too far on the right side",
//                ts(10, 0, 11, 0),
//                ts(11, 1, 12, 0),
//                listOf(
//                    ts(10, 0, 11, 0),
//                ),
//            ),
//        ).forEach { (title, source, other, expected) ->
//
//            "$title: ${source.format()} cut away ${other.format()} must be ${expected.format()}" {
//
//                source.cutAway(other) shouldBe expected
//            }
//        }
//    }
//
//    "splitByMinutes()" - {
//
//        listOf(
//            row(
//                "Edge case: empty initial slot",
//                ts(10, 0, 10, 0), 10, 0, listOf()
//            ),
//            row(
//                "Edge case: duration < 1",
//                ts(10, 0, 11, 0), 0, 0, listOf()
//            ),
//            row(
//                "Edge case: gap < 0",
//                ts(10, 0, 11, 0), 30, -10,
//                listOf(
//                    ts(10, 0, 10, 30),
//                    ts(10, 30, 11, 0),
//                )
//            ),
//            row(
//                "Normal case: duration = 15 gap = 5",
//                ts(10, 0, 11, 0), 15, 5,
//                listOf(
//                    ts(10, 0, 10, 15),
//                    ts(10, 20, 10, 35),
//                    ts(10, 40, 10, 55),
//                )
//            ),
//            row(
//                "Normal case: duration = 1 gap = 0",
//                ts(10, 0, 10, 5), 1, 0,
//                listOf(
//                    ts(10, 0, 10, 1),
//                    ts(10, 1, 10, 2),
//                    ts(10, 2, 10, 3),
//                    ts(10, 3, 10, 4),
//                    ts(10, 4, 10, 5),
//                )
//            ),
//        ).forEach { (title, initial, duration, gap, expected) ->
//
//            "$title: ${initial.format()} splitByMinutes ($duration, $gap) must be ${expected.format()}" {
//
//                initial.splitByMinutes(duration, gap) shouldBe expected
//            }
//        }
//    }
})
