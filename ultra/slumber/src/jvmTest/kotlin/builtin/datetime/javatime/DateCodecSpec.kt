package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import java.util.*

class DateCodecSpec : StringSpec({

    val codec = Codec.default
    // 2022-04-05T00:00:00Z
    val epochMs = 1649116800000L

    data class Wrapper(val date: Date)

    "Slumber - pure" {
        val subject = Date(epochMs)

        @Suppress("UNCHECKED_CAST")
        val result = codec.slumber(subject) as Map<String, Any>

        result["ts"] shouldBe epochMs
        result["timezone"] shouldBe "UTC"
    }

    "Slumber - wrapped" {
        val subject = Wrapper(Date(epochMs))

        @Suppress("UNCHECKED_CAST")
        val result = codec.slumber(subject) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val inner = result["date"] as Map<String, Any>

        inner["ts"] shouldBe epochMs
        inner["timezone"] shouldBe "UTC"
    }

    "Awake - pure" {
        val input = mapOf("ts" to epochMs, "timezone" to "UTC")
        val result = codec.awake<Date>(input)

        result shouldBe Date(epochMs)
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<Date?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf(
            "date" to mapOf("ts" to epochMs, "timezone" to "UTC", "human" to "...")
        )
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(Date(epochMs))
    }

    "Roundtrip" {
        val start = Date(epochMs)
        val result = codec.awake<Date>(codec.slumber(start))

        result shouldBe start
    }
})
