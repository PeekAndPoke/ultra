package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalTime

/** ULTRA-00316 track A batch 3a: `EndOfDay` and java.time, which cannot hold 24:00. */
@Suppress("unused")
class MpLocalTimeEndOfDayJvmSpec : StringSpec({

    "EndOfDay.jvm is LocalTime.MAX, the last instant of the day; the reverse never gives EndOfDay" {
        MpLocalTime.EndOfDay.jvm shouldBe LocalTime.MAX
        LocalTime.MAX.mp shouldBe MpLocalTime.of(23, 59, 59)
    }

    "pre-existing, left for a follow-up: .jvm drops the milliseconds" {
        MpLocalTime.Max.jvm shouldBe LocalTime.of(23, 59, 59)
    }
})
