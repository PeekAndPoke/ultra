package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class MapSlumbererSpec : StringSpec({

    "Slumbering an emptyMap must work" {

        val data = emptyMap<Any, Any>()

        val codec = Codec.default

        val result = codec.slumber(data)

        result shouldBe emptyMap<Any, Any>()
    }

    "Slumbering a Map of String-2-Int must work" {

        val subject = MapSlumberer(kType<String>().type, kType<Int>().type)
        val codec = Codec.default
        val result = subject.slumber(mapOf("a" to 1, 2 to 2), codec.secondPassSlumbererContext)!!

        result shouldBe mapOf("a" to 1, "2" to 2)
    }

    "Slumbering a Map of String-2-Instant must work" {

        val subject = MapSlumberer(kType<String>().type, kType<LocalDateTime>().type)
        val codec = Codec.default
        val data = mapOf(
            "a" to LocalDateTime.of(2020, 1, 22, 0, 0),
            2 to LocalDateTime.of(2000, 1, 1, 0, 0)
        )

        val result = subject.slumber(data, codec.secondPassSlumbererContext)!!

        result shouldBe mapOf(
            "a" to mapOf(
                "ts" to 1579651200000L,
                "timezone" to "UTC",
                "human" to "2020-01-22T00:00Z[UTC]"
            ),
            "2" to mapOf(
                "ts" to 946684800000L,
                "timezone" to "UTC",
                "human" to "2000-01-01T00:00Z[UTC]"
            )
        )
    }

    "Slumbering a generic Map with nested null values" {

        val codec = Codec.default

        val data = mapOf(
            "a" to 1,
            "b" to null,
            "c" to mapOf(
                "a" to null,
                "b" to listOf("a", null),
            )
        )

        val result = codec.slumber(data)

        result shouldBe data
    }
})
