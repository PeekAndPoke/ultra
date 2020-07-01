package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class KlockDateCodecSpec : StringSpec({

    "Awaking a Date from valid data must work" {

        val codec = Codec.default

        val data = mapOf(
            "ts" to 282828282_000
        )

        val result = codec.awake(Date::class, data)

        result shouldBe DateTime(282828282_000).date
    }

    "Awaking a Date child property must work" {

        data class DataClass(val date: Date)

        val codec = Codec.default

        val data = mapOf(
            "date" to mapOf(
                "ts" to 282828282_000
            )
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(DateTime(282828282_000).date)
    }

    "Awaking a Date from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<DateTime?>(data)

        result.shouldBeNull()
    }

    "Slumbering a Date must work" {

        val codec = Codec.default

        val data = DateTime(282828282_000).date

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "ts" to 282787200_000,
            "timezone" to "UTC",
            "human" to "1978-12-18T00:00:00.000Z"
        )
    }

    "Slumbering a Date child property must work" {

        data class DataClass(val date: Date)

        val codec = Codec.default

        val data = DataClass(DateTime(282828282_000).date)

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 282787200_000,
                "timezone" to "UTC",
                "human" to "1978-12-18T00:00:00.000Z"
            )
        )
    }
})
