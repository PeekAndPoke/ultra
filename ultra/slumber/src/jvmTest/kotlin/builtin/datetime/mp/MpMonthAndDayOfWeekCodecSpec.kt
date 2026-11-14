package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpDayOfWeek
import io.peekandpoke.ultra.datetime.MpMonth
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

/** ULTRA-00210: MpMonth and MpDayOfWeek as fields survive a slumber round trip, stored by name. */
class MpMonthAndDayOfWeekCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val month: MpMonth, val dayOfWeek: MpDayOfWeek)

    "every month and day of week round-trips by name" {
        MpMonth.entries.forEach { month ->
            MpDayOfWeek.entries.forEach { day ->
                val input = Wrapper(month = month, dayOfWeek = day)

                val slumbered = codec.slumber(input)

                slumbered shouldBe mapOf("month" to month.name, "dayOfWeek" to day.name)
                codec.awake(Wrapper::class, slumbered) shouldBe input
            }
        }
    }
})
