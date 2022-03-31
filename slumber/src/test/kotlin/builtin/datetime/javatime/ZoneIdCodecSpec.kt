package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.ZoneId

class ZoneIdCodecSpec : StringSpec({

    "Awaking a ZoneId from valid string must work" {

        val codec = Codec.default

        val result = codec.awake(ZoneId::class, "Europe/Berlin")

        result shouldBe ZoneId.of("Europe/Berlin")
    }

    "Awaking a ZoneId child property must work" {

        data class DataClass(val zone: ZoneId)

        val codec = Codec.default

        val data = mapOf(
            "zone" to "UTC"
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(ZoneId.of("UTC"))
    }

    "Awaking a ZoneId from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<ZoneId?>(data)

        result.shouldBeNull()
    }

    "Awaking a ZoneId from invalid id must return null" {

        val codec = Codec.default

        val data = "Invalid/ZoneId"

        val result = codec.awake<ZoneId?>(data)

        result.shouldBeNull()
    }

    "Slumbering a ZoneId must work" {

        val codec = Codec.default

        val data = ZoneId.of("Europe/Berlin")

        val result = codec.slumber(data)

        result shouldBe "Europe/Berlin"
    }
})
