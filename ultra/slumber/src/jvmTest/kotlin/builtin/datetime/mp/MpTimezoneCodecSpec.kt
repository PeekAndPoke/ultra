package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

class MpTimezoneCodecSpec : StringSpec({

    "Awaking a Timezone from valid data must work" {

        val codec = Codec.default

        val data = "Europe/Berlin"

        val result = codec.awake(MpTimezone::class, data)

        result shouldBe MpTimezone.of("Europe/Berlin")
    }

    "Awaking a Timezone child property must work" {

        data class DataClass(val zone: MpTimezone)

        val codec = Codec.default

        val data = mapOf(
            "zone" to "Europe/Berlin"
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(MpTimezone.of("Europe/Berlin"))

        result!!.zone.kotlinx shouldBe TimeZone.of("Europe/Berlin")
    }

    "Awaking a Timezone from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<MpTimezone?>(data)

        result.shouldBeNull()
    }

    "Slumbering a Timezone must work" {

        val codec = Codec.default

        val data = MpTimezone.of("Europe/Berlin")

        val result = codec.slumber(data)

        result shouldBe "Europe/Berlin"
    }

    "Slumbering a Timezone child property must work" {

        data class DataClass(val zone: MpTimezone)

        val codec = Codec.default

        val data = DataClass(MpTimezone.of("Europe/Berlin"))

        val result = codec.slumber(data)

        result shouldBe mapOf(
            "zone" to "Europe/Berlin"
        )
    }
})
