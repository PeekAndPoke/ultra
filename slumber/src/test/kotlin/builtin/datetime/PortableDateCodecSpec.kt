package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.common.datetime.PortableDate
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PortableDateCodecSpec : StringSpec({

    "Awaking a Date from valid data must work" {

        val codec = Codec.default

        val data = mapOf(
            "ts" to 282828282_000
        )

        val result = codec.awake(PortableDate::class, data)

        result shouldBe PortableDate(282828282_000)
    }

    "Awaking a Date child property must work" {

        data class DataClass(val date: PortableDate)

        val codec = Codec.default

        val data = mapOf(
            "date" to mapOf(
                "ts" to 282828282_000
            )
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(PortableDate(282828282_000))
    }

    "Awaking a Date from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<PortableDate?>(data)

        result.shouldBeNull()
    }

    "Slumbering a Date must work" {

        val codec = Codec.default

        val data = PortableDate(282828282_000)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "ts" to 282828282_000,
            "timezone" to "UTC",
            "human" to "1978-12-18T00:00:00Z[UTC]"
        )
    }

    "Slumbering a Date child property must work" {

        data class DataClass(val date: PortableDate)

        val codec = Codec.default

        val data = DataClass(PortableDate(282828282_000))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 282828282_000,
                "timezone" to "UTC",
                "human" to "1978-12-18T00:00:00Z[UTC]"
            )
        )
    }
})
