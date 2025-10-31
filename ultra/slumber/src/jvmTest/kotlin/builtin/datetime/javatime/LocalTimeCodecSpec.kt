package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalTime

class LocalTimeCodecSpec : StringSpec({

    "Awaking a LocalTime from valid number must work" {

        val codec = Codec.default

        val result = codec.awake(LocalTime::class, 45015)

        result shouldBe LocalTime.of(12, 30, 15)
    }

    "Awaking a LocalTime from valid number must work #2" {

        val codec = Codec.default

        val result = codec.awake(LocalTime::class, 0)

        result shouldBe LocalTime.of(0, 0, 0)
    }

    "Awaking a LocalTime from valid number must work #3" {

        val codec = Codec.default

        val result = codec.awake(LocalTime::class, 86399)

        result shouldBe LocalTime.of(23, 59, 59)
    }

    "Awaking a LocalTime child property must work" {

        data class DataClass(val time: LocalTime)

        val codec = Codec.default

        val data = mapOf(
            "time" to 45015
        )

        val result = codec.awake(DataClass::class, data)

        result shouldBe DataClass(LocalTime.of(12, 30, 15))
    }

    "Awaking a LocalTime from invalid data must return null" {

        val codec = Codec.default

        val data = mapOf<String, String>()

        val result = codec.awake<LocalTime?>(data)

        result.shouldBeNull()
    }

    "Awaking a LocalTime from invalid value must return null" {

        val codec = Codec.default

        val data = 86400

        val result = codec.awake<LocalTime?>(data)

        result.shouldBeNull()
    }

    "Awaking a LocalTime from invalid value must return null #2" {

        val codec = Codec.default

        val data = -1

        val result = codec.awake<LocalTime?>(data)

        result.shouldBeNull()
    }

    "Slumbering a ZoneId must work" {

        val codec = Codec.default

        val data = LocalTime.of(12, 30, 15)

        val result = codec.slumber(data)

        result shouldBe 45015
    }
})
