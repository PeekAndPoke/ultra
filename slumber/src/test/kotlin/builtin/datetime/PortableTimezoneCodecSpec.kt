package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.common.datetime.PortableTimezone
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class PortableTimezoneCodecSpec : StringSpec({

    "Awaking a Timezone from valid data must work" {

        val codec = Codec.default

        val data = "Europe/Berlin"

        val result = codec.awake(PortableTimezone::class, data)

        result shouldBe PortableTimezone("Europe/Berlin")
    }

    "Awaking a Timezone child property must work" {

        data class DataClass(val zone: PortableTimezone)

        val codec = Codec.default

        val data = mapOf(
            "zone" to "Europe/Berlin"
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(PortableTimezone("Europe/Berlin"))
    }

    "Awaking a Timezone from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<PortableTimezone?>(data)

        result.shouldBeNull()
    }

    "Slumbering a Timezone must work" {

        val codec = Codec.default

        val data = PortableTimezone("Europe/Berlin")

        val result = codec.slumber(data)

        result shouldBe "Europe/Berlin"
    }

    "Slumbering a Timezone child property must work" {

        data class DataClass(val zone: PortableTimezone)

        val codec = Codec.default

        val data = DataClass(PortableTimezone("Europe/Berlin"))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "zone" to "Europe/Berlin"
        )
    }
})
