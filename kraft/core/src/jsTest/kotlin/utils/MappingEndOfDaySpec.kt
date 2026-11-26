package io.peekandpoke.kraft.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpLocalTime

/**
 * Current behaviour, pinned (ULTRA-00341, no kraft change): what the time-field mapping does with
 * `MpLocalTime.EndOfDay` (24:00).
 */
class MappingEndOfDaySpec : StringSpec({

    "timeToHms renders EndOfDay as 24:00:00; the parsers read it back" {
        timeToHms(MpLocalTime.EndOfDay) shouldBe "24:00:00"
        stringToLocalTime("24:00:00") shouldBe MpLocalTime.EndOfDay
        stringToLocalTimeOrNull("24:00:00") shouldBe MpLocalTime.EndOfDay
        stringToLocalTimeOrNull("24:30:00") shouldBe null
    }
})
