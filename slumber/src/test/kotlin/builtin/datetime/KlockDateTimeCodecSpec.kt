package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.DateTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class KlockDateTimeCodecSpec : StringSpec({

    "Awaking a DateTime from valid data must work" {

        val codec = Codec.default

        val data = mapOf(
            "ts" to 282828282_000
        )

        val result = codec.awake(DateTime::class, data)

        result shouldBe DateTime(282828282_000)
    }

    "Awaking a DateTime child property must work" {

        data class DataClass(val date: DateTime)

        val codec = Codec.default

        val data = mapOf(
            "date" to mapOf(
                "ts" to 282828282_000
            )
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(DateTime(282828282_000))
    }

    "Awaking a DateTime from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<DateTime?>(data)

        result.shouldBeNull()
    }

    "Slumbering a DateTime must work" {

        val codec = Codec.default

        val data = DateTime(282828282_000)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "ts" to 282828282_000,
            "timezone" to "UTC",
            "human" to "1978-12-18T11:24:42.000Z"
        )
    }

    "Slumbering a DateTime child property must work" {

        data class DataClass(val date: DateTime)

        val codec = Codec.default

        val data = DataClass(DateTime(282828282_000))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 282828282_000,
                "timezone" to "UTC",
                "human" to "1978-12-18T11:24:42.000Z"
            )
        )
    }
})
