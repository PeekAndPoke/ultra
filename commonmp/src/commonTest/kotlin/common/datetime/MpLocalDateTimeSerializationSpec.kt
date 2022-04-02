package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.serialization.json.Json

@Suppress("unused")
class MpLocalDateTimeSerializationSpec : StringSpec({

    val json = Json

    @kotlinx.serialization.Serializable
    data class Wrapper(val date: MpLocalDateTime)

    "Serializing - pure" {

        val result = json.encodeToString(
            MpLocalDateTime.serializer(),
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
        )

        result shouldBe """
            {"ts":1649160794000,"timezone":"UTC","human":"2022-04-05T12:13:14.000Z"}
        """.trimIndent()
    }

    "Serializing - wrapped" {

        val result = json.encodeToString(
            Wrapper.serializer(),
            Wrapper(
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
            )
        )

        result shouldBe """
            {"date":{"ts":1649160794000,"timezone":"UTC","human":"2022-04-05T12:13:14.000Z"}}
        """.trimIndent()
    }

    "De-Serializing - pure" {

        val result = json.decodeFromString(
            MpLocalDateTime.serializer(),
            """
                {"ts":1649160794000,"timezone":"UTC","human":"2022-04-05T12:13:14"}
            """.trimIndent()
        )

        result shouldBe MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
    }

    "De-Serializing - wrapped" {

        val result = json.decodeFromString(
            Wrapper.serializer(),
            """
                {"date":{"ts":1649160794000,"timezone":"UTC","human":"2022-04-05T12:13:14"}}
            """.trimIndent()
        )

        result shouldBe Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
        )
    }

    "Roundtrip - pure" {

        val start: MpLocalDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5)

        val result: MpLocalDateTime = json.decodeFromString(
            MpLocalDateTime.serializer(),
            json.encodeToString(
                MpLocalDateTime.serializer(),
                start
            )
        )

        result shouldBe start
    }
})
