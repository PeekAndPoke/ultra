package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class LocalDateCodecSpec : StringSpec({

    val codec = Codec.default
    val date = LocalDate.of(2022, 4, 5)
    // 2022-04-05 at start of day UTC
    val epochMs = 1649116800000L

    data class Wrapper(val date: LocalDate)

    "Slumber - pure" {
        val result = codec.slumber(date)

        @Suppress("UNCHECKED_CAST")
        val map = result as Map<String, Any>
        map["ts"] shouldBe epochMs
        map["timezone"] shouldBe "UTC"
    }

    "Awake - pure" {
        val result = codec.awake<LocalDate>(mapOf("ts" to epochMs, "timezone" to "UTC"))

        result shouldBe date
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<LocalDate?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf("date" to mapOf("ts" to epochMs, "timezone" to "UTC"))
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(date)
    }

    "Roundtrip" {
        val result = codec.awake<LocalDate>(codec.slumber(date))

        result shouldBe date
    }
})
