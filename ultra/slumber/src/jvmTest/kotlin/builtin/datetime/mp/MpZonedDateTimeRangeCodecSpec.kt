package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTimeRange
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

/** ULTRA-00211: a range stored after atZone(tz) (ts = Doomsday, timezone = tz) decodes without an end. */
class MpZonedDateTimeRangeCodecSpec : StringSpec({

    val codec = Codec.default

    "a range stored after atZone(America/New_York) round-trips with hasEnd false" {
        val range = MpZonedDateTimeRange
            .beginningAt(MpInstant.parse("2022-07-01T10:00:00Z").atZone(MpTimezone.of("Europe/Berlin")))
            .atZone(MpTimezone.of("America/New_York"))

        val decoded = codec.awake(MpZonedDateTimeRange::class, codec.slumber(range))

        decoded?.hasEnd shouldBe false
        decoded shouldBe range
    }
})
