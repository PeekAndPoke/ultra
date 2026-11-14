package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

/** ULTRA-00316 track A batch 3a: `MpLocalTime.EndOfDay` is stored as 86 400 000 milliseconds of day. */
class MpLocalTimeEndOfDayCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val time: MpLocalTime)

    "EndOfDay is slumbered as the Long 86400000 and awakes as EndOfDay" {
        codec.slumber(MpLocalTime.EndOfDay) shouldBe 86_400_000L
        codec.awake<MpLocalTime>(codec.slumber(MpLocalTime.EndOfDay)) shouldBe MpLocalTime.EndOfDay
    }

    "only a number that is exactly one day gives EndOfDay: Int, Long, whole Double; a fractional Double wraps" {
        codec.awake<MpLocalTime>(86_400_000) shouldBe MpLocalTime.EndOfDay
        codec.awake<MpLocalTime>(86_400_000L) shouldBe MpLocalTime.EndOfDay
        codec.awake<MpLocalTime>(86_400_000.0) shouldBe MpLocalTime.EndOfDay
        codec.awake<MpLocalTime>(86_400_000.5) shouldBe MpLocalTime.Min
    }

    "every other out-of-range number still wraps, as before" {
        codec.awake<MpLocalTime>(86_400_001L) shouldBe MpLocalTime.of(0, 0, 0, 1)
        codec.awake<MpLocalTime>(172_800_000L) shouldBe MpLocalTime.Min
        codec.awake<MpLocalTime>(-86_400_000L) shouldBe MpLocalTime.Min
    }

    "a field holding EndOfDay round-trips" {
        val input = Wrapper(MpLocalTime.EndOfDay)

        val slumbered = codec.slumber(input)

        slumbered shouldBe mapOf("time" to 86_400_000L)
        codec.awake(Wrapper::class, slumbered) shouldBe input
    }
})
