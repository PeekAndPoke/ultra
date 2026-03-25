package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ZonedDateTimeCodecSpec : StringSpec({

    val codec = Codec.default
    val epochMs = 1649116800000L
    val berlinZone = ZoneId.of("Europe/Berlin")
    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), berlinZone)

    data class Wrapper(val dt: ZonedDateTime)

    "Slumber - pure" {
        val result = codec.slumber(zdt)

        result shouldBe mapOf(
            "ts" to epochMs,
            "timezone" to "Europe/Berlin",
            "human" to zdt.toString(),
        )
    }

    "Awake - pure with timezone" {
        val input = mapOf("ts" to epochMs, "timezone" to "Europe/Berlin")
        val result = codec.awake<ZonedDateTime>(input)

        result shouldBe zdt
    }

    "Awake - missing timezone returns null" {
        val input = mapOf("ts" to epochMs)
        val result = codec.awake<ZonedDateTime?>(input)

        result shouldBe null
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<ZonedDateTime?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf("dt" to mapOf("ts" to epochMs, "timezone" to "Europe/Berlin"))
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(zdt)
    }

    "Roundtrip preserves timezone" {
        val result = codec.awake<ZonedDateTime>(codec.slumber(zdt))

        result shouldBe zdt
        result!!.zone shouldBe berlinZone
    }
})
