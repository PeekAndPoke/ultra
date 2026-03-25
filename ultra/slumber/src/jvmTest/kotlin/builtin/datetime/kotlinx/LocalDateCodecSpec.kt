package de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class LocalDateCodecSpec : StringSpec({

    val codec = Codec.default
    val date = LocalDate(2022, Month.APRIL, 5)
    val epochMs = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    data class Wrapper(val date: kotlinx.datetime.LocalDate)

    "Slumber - pure" {
        val result = codec.slumber(date)

        @Suppress("UNCHECKED_CAST")
        val map = result as Map<String, Any>
        map["ts"] shouldBe epochMs
        map["timezone"] shouldBe "UTC"
    }

    "Awake - pure" {
        val result = codec.awake<kotlinx.datetime.LocalDate>(
            mapOf("ts" to epochMs, "timezone" to "UTC")
        )

        result shouldBe date
    }

    "Awake - from non-map returns null" {
        val result = codec.awake<kotlinx.datetime.LocalDate?>("not a map")

        result shouldBe null
    }

    "Awake - wrapped" {
        val input = mapOf("date" to mapOf("ts" to epochMs, "timezone" to "UTC"))
        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(date)
    }

    "Roundtrip" {
        val result = codec.awake<kotlinx.datetime.LocalDate>(codec.slumber(date))

        result shouldBe date
    }
})
