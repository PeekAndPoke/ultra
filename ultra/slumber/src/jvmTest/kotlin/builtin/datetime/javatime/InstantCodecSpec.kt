package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class InstantCodecSpec : StringSpec({

    val codec = Codec.default
    val epochMs = 1649116800000L
    val instant = Instant.ofEpochMilli(epochMs)

    data class Wrapper(val ts: Instant)

    "Slumber - pure" {
        val result = codec.slumber(instant)

        result shouldBe mapOf(
            "ts" to epochMs,
            "timezone" to "UTC",
            "human" to instant.toString(),
        )
    }

    "Slumber - wrapped" {
        val result = codec.slumber(Wrapper(instant))

        result shouldBe mapOf(
            "ts" to mapOf(
                "ts" to epochMs,
                "timezone" to "UTC",
                "human" to instant.toString(),
            )
        )
    }

    "Awake - pure" {
        val result = codec.awake<Instant>(mapOf("ts" to epochMs, "timezone" to "UTC"))

        result shouldBe instant
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<Instant?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf("ts" to mapOf("ts" to epochMs, "timezone" to "UTC"))
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(instant)
    }

    "Roundtrip" {
        val result = codec.awake<Instant>(codec.slumber(instant))

        result shouldBe instant
    }
})
