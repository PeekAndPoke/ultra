package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

@Suppress("unused")
class MpTimezoneSerializationSpec : StringSpec({

    val json = Json

    @kotlinx.serialization.Serializable
    data class Wrapper(val zone: MpTimezone)

    "Serializing - pure" {

        val result = json.encodeToString(
            MpTimezone.serializer(),
            MpTimezone.of("Europe/Berlin"),
        )

        result shouldBe """
            "Europe/Berlin"
        """.trimIndent()
    }

    "Serializing - wrapped" {

        val result = json.encodeToString(
            Wrapper.serializer(),
            Wrapper(
                MpTimezone.of("Europe/Berlin"),
            )
        )

        result shouldBe """
            {"zone":"Europe/Berlin"}
        """.trimIndent()
    }

    "De-Serializing - pure - UTC" {

        val result = json.decodeFromString(
            MpTimezone.serializer(),
            """
                "UTC"
            """.trimIndent()
        )

        result shouldBe MpTimezone.of("UTC")
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val result = json.decodeFromString(
            MpTimezone.serializer(),
            """
                "Europe/Bucharest"
            """.trimIndent()
        )

        result shouldBe MpTimezone.of("Europe/Bucharest")
    }

    "De-Serializing - wrapped" {

        val result = json.decodeFromString(
            Wrapper.serializer(),
            """
                {"zone": "Europe/Bucharest"}
            """.trimIndent()
        )

        result shouldBe Wrapper(
            MpTimezone.of("Europe/Bucharest"),
        )
    }

    "Roundtrip - pure" {

        val start: MpTimezone = MpTimezone.of("Europe/Berlin")

        val result: MpTimezone = json.decodeFromString(
            MpTimezone.serializer(),
            json.encodeToString(
                MpTimezone.serializer(),
                start
            )
        )

        result shouldBe start
    }
})
