package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.common.datetime.PortableTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PortableTimeCodecSpec : StringSpec({

    "Awaking a Time from valid data must work" {

        val codec = Codec.default

        val data = 45_000_000

        val result = codec.awake(PortableTime::class, data)

        result shouldBe PortableTime(45_000_000)
    }

    "Awaking a Time child property must work" {

        data class DataClass(val time: PortableTime)

        val codec = Codec.default

        val data = mapOf(
            "time" to 45_000
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(PortableTime(45_000))
    }

    "Awaking a Time from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<PortableTime?>(data)

        result.shouldBeNull()
    }

    "Slumbering a Time must work" {

        val codec = Codec.default

        val data = PortableTime(45_000_000)

        val result = codec.slumber(data)

        result shouldBe 45_000_000
    }

    "Slumbering a Time child property must work" {

        data class DataClass(val time: PortableTime)

        val codec = Codec.default

        val data = DataClass(PortableTime(45_000_000))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "time" to 45_000_000
        )
    }
})
