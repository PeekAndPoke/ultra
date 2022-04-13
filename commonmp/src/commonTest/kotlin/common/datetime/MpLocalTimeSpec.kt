package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class MpLocalTimeSpec : StringSpec({

    "parse" {
        MpLocalTime.parse("12:00") shouldBe
                MpLocalTime.of(hour = 12, minute = 0)

        MpLocalTime.parse("12:13:14") shouldBe
                MpLocalTime.of(hour = 12, minute = 13, second = 14)

        MpLocalTime.parse("12:13:14.123") shouldBe
                MpLocalTime.of(hour = 12, minute = 13, second = 14, milliSecond = 123)
    }

    "inWholeMilliSeconds" {
        MpLocalTime.parse("00:00:00.000").inWholeMilliSeconds() shouldBe
                (0 * 60 * 60 + 0 * 60 + 0) * 1_000 + 0

        MpLocalTime.parse("12:13:14.123").inWholeMilliSeconds() shouldBe
                (12 * 60 * 60 + 13 * 60 + 14) * 1_000 + 123

        MpLocalTime.parse("23:59:59.999").inWholeMilliSeconds() shouldBe
                (23 * 60 * 60 + 59 * 60 + 59) * 1_000 + 999
    }

    "properties - hours" {
        (0..48).forEach { hour ->

            val subject = MpLocalTime.of(hour, 13, 14, 123)
            val expected = hour % 24

            withClue("Hours of '${subject.toIsoString()}' should be '$expected'") {
                subject.hour shouldBe expected
            }
        }
    }

    "toString" {
        MpLocalTime.of(12, 13, 14, 123)
            .toString() shouldBe "MpLocalTime(12:13:14.123)"
    }

    "toIsoString" {
        MpLocalTime.of(12, 13, 14, 123)
            .toIsoString() shouldBe "12:13:14.123"
    }

    "format" {
        MpLocalTime.of(12, 13, 14, 123)
            .format("HH mm ss SSS") shouldBe "12 13 14 123"
    }

    "formatHhMm" {
        MpLocalTime.of(12, 13, 14, 123)
            .formatHhMm() shouldBe "12:13"
    }

    "formatHhMmSs" {
        MpLocalTime.of(12, 13, 14, 123)
            .formatHhMmSs() shouldBe "12:13:14"
    }

})
