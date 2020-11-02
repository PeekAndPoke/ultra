package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.common.datetime.PortableDateTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PortableDateTimeCodecSpec : StringSpec({

    "Awaking a DateTime from valid data must work" {

        val codec = Codec.default

        val data = mapOf(
            "ts" to 282828282_000
        )

        val result = codec.awake(PortableDateTime::class, data)

        result shouldBe PortableDateTime(282828282_000)
    }

    "Awaking a DateTime child property must work" {

        data class DataClass(val date: PortableDateTime)

        val codec = Codec.default

        val data = mapOf(
            "date" to mapOf(
                "ts" to 282828282_000
            )
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(PortableDateTime(282828282_000))
    }

    "Awaking a DateTime from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<PortableDateTime?>(data)

        result.shouldBeNull()
    }

    "Slumbering a DateTime must work" {

        val codec = Codec.default

        val data = PortableDateTime(282828282_000)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "ts" to 282828282_000,
            "timezone" to "UTC",
            "human" to "1978-12-18T11:24:42Z[UTC]"
        )
    }

    "Slumbering a DateTime child property must work" {

        data class DataClass(val date: PortableDateTime)

        val codec = Codec.default

        val data = DataClass(PortableDateTime(282828282_000))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 282828282_000,
                "timezone" to "UTC",
                "human" to "1978-12-18T11:24:42Z[UTC]"
            )
        )
    }
})
