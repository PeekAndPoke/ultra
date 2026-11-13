package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

/** ULTRA-00211: a slumber round trip keeps the instant on both sides of a DST overlap and after a gap. */
class MpZonedDateTimeOverlapCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val date: MpZonedDateTime)

    listOf(
        "overlap, first half" to 1_667_089_800_000L,
        "overlap, second half" to 1_667_093_400_000L,
        "after a gap" to 1_648_344_600_000L,
    ).forEach { (case, ts) ->

        "$case: a slumber round trip gives back the same instant and an equal value" {
            val input = Wrapper(MpInstant.fromEpochMillis(ts).atZone(MpTimezone.of("Europe/Berlin")))

            val slumbered = codec.slumber(input)
            val result = codec.awake(Wrapper::class, slumbered)

            result?.date?.toEpochMillis() shouldBe ts
            result shouldBe input
        }
    }
})
