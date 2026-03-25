package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

class MpLocalTimeCodecSpec : StringSpec({

    val codec = Codec.default

    "Slumber" {

        val subject = MpLocalTime.ofMilliSeconds(123)

        val result = codec.slumber(subject)

        result shouldBe 123
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
