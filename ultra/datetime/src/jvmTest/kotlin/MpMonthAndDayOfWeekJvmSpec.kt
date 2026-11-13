package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.Month

/** ULTRA-00210: the java.time converters of the Mp month and day-of-week types. */
@Suppress("unused")
class MpMonthAndDayOfWeekJvmSpec : StringSpec({

    "java.time converters, both ways, for every entry" {
        Month.entries.forEach { month ->
            month.mp.number shouldBe month.value
            month.mp.jvm shouldBe month
        }
        DayOfWeek.entries.forEach { day ->
            day.mp.isoDayNumber shouldBe day.value
            day.mp.jvm shouldBe day
        }
    }
})
