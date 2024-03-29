package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpLocalTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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
