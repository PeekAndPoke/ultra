package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class MpLocalTimeSpec : StringSpec({
    "Creation" {
        MpLocalTime(123).milliSeconds shouldBe 123

        MpLocalTime.of(secondOfDay = 10).milliSeconds shouldBe 10 * 1000
    }
})
