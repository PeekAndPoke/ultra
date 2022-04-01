package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json

@Suppress("unused")
class MpZonedDateTimeSerializationSpec : StringSpec({

    val json = Json

    @kotlinx.serialization.Serializable
    data class Wrapper(val date: MpZonedDateTime)

    "Serializing - pure - UTC" {

        val result = json.encodeToString(
            MpZonedDateTime.serializer(),
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("UTC")),
        )

        result shouldBe """
            {"ts":1649160794000,"timezone":"Z","human":"2022-04-05T12:13:14Z"}
        """.trimIndent()
    }

    "Serializing - pure - Europe/Bucharest" {

        val result = json.encodeToString(
            MpZonedDateTime.serializer(),
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("Europe/Bucharest")),
        )

        result shouldBe """
            {"ts":1649149994000,"timezone":"Europe/Bucharest","human":"2022-04-05T12:13:14[Europe/Bucharest]"}
        """.trimIndent()
    }

    "Serializing - wrapped - Europe/Bucharest" {

        val result = json.encodeToString(
            Wrapper.serializer(),
            Wrapper(
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                    .atZone(TimeZone.of("Europe/Bucharest")),
            )
        )

        result shouldBe """
            {"date":{"ts":1649149994000,"timezone":"Europe/Bucharest","human":"2022-04-05T12:13:14[Europe/Bucharest]"}}
        """.trimIndent()
    }

    "De-Serializing - pure - UTC" {

        val result = json.decodeFromString(
            MpZonedDateTime.serializer(),
            """
                {"ts":1649160794000,"timezone":"Z","human":""}
            """.trimIndent()
        )

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                    .atZone(TimeZone.UTC)
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val result = json.decodeFromString(
            MpZonedDateTime.serializer(),
            """
                {"ts":1649149994000,"timezone":"Europe/Bucharest","human":""}
            """.trimIndent()
        )

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                    .atZone(TimeZone.of("Europe/Bucharest"))
    }

    "De-Serializing - wrapped - Europe/Bucharest" {

        val result = json.decodeFromString(
            Wrapper.serializer(),
            """
                {"date":{"ts":1649149994000,"timezone":"Europe/Bucharest","human":""}}
            """.trimIndent()
        )

        result shouldBe Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("Europe/Bucharest")),
        )
    }
})
