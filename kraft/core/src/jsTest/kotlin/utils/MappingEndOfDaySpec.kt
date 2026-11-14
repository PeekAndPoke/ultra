package io.peekandpoke.kraft.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpLocalTime
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement

/**
 * Current behaviour, pinned (ULTRA-00316 track A batch 3a, no kraft change): what the time-field mapping does with
 * `MpLocalTime.EndOfDay` (24:00), and what the browser's `<input type="time">` does with it.
 */
class MappingEndOfDaySpec : StringSpec({

    "timeToHms renders EndOfDay as 24:00:00; the parsers read it back" {
        timeToHms(MpLocalTime.EndOfDay) shouldBe "24:00:00"
        stringToLocalTime("24:00:00") shouldBe MpLocalTime.EndOfDay
        stringToLocalTimeOrNull("24:00:00") shouldBe MpLocalTime.EndOfDay
        stringToLocalTimeOrNull("24:30:00") shouldBe null
    }

    "the browser's time input drops 24:00:00, so a kraft time field shows EndOfDay as empty" {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "time"

        input.value = "24:00:00"
        input.value shouldBe ""

        input.value = "23:59:59"
        input.value shouldBe "23:59:59"
    }
})
