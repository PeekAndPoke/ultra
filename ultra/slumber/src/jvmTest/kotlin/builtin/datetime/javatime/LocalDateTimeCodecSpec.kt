package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import java.time.LocalDateTime
import java.time.Month

class LocalDateTimeCodecSpec : StringSpec({

    val codec = Codec.default
    val dateTime = LocalDateTime.of(2022, Month.APRIL, 5, 10, 30, 0)
    // 2022-04-05T10:30:00 UTC
    val epochMs = 1649154600000L

    data class Wrapper(val dt: LocalDateTime)

    "Slumber - pure" {
        val result = codec.slumber(dateTime)

        @Suppress("UNCHECKED_CAST")
        val map = result as Map<String, Any>
        map["ts"] shouldBe epochMs
        map["timezone"] shouldBe "UTC"
    }

    "Awake - pure" {
        val result = codec.awake<LocalDateTime>(mapOf("ts" to epochMs, "timezone" to "UTC"))

        result shouldBe dateTime
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<LocalDateTime?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf("dt" to mapOf("ts" to epochMs, "timezone" to "UTC"))
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(dateTime)
    }

    "Roundtrip" {
        val result = codec.awake<LocalDateTime>(codec.slumber(dateTime))

        result shouldBe dateTime
    }
})
