package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

class MpLocalTimeCodecSpec : StringSpec({

    val codec = Codec.default

    "Slumber" {

        val subject = MpLocalTime.ofMilliSeconds(123)

        val result = codec.slumber(subject)

        // a Long, not an Int: the stored number type (Int64) must not change with MpLocalTime's Int storage
        result.shouldBeInstanceOf<Long>()
        result shouldBe 123L
    }

    "De-Serializing wraps values outside the day, also beyond the Int range" {
        codec.awake<MpLocalTime>(-1000) shouldBe MpLocalTime.of(23, 59, 59)
        codec.awake<MpLocalTime>(5_000_000_000L) shouldBe MpLocalTime.of(20, 53, 20)
    }

    "De-Serializing" {

        val input = 123

        val result = codec.awake<MpLocalTime>(input)

        result shouldBe MpLocalTime.ofMilliSeconds(123)
    }

    "Roundtrip - pure" {

        val start = MpLocalTime.ofMilliSeconds(123)

        val result = codec.awake<MpLocalTime>(
            codec.slumber(start)
        )

        result shouldBe start
    }
})
