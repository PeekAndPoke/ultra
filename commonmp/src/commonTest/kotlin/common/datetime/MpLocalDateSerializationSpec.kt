package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.serialization.json.Json

@Suppress("unused")
class MpLocalDateSerializationSpec : StringSpec({

    val json = Json

    @kotlinx.serialization.Serializable
    data class Wrapper(val date: MpLocalDate)

    "Serializing - pure" {

        val result = json.encodeToString(
            MpLocalDate.serializer(),
            MpLocalDate.of(2022, Month.APRIL, 5),
        )

        result shouldBe """
            {"ts":1649116800000,"timezone":"UTC","human":"2022-04-05"}
        """.trimIndent()
    }

    "Serializing - wrapped" {

        val result = json.encodeToString(
            Wrapper.serializer(),
            Wrapper(
                MpLocalDate.of(2022, Month.APRIL, 5),
            )
        )

        result shouldBe """
            {"date":{"ts":1649116800000,"timezone":"UTC","human":"2022-04-05"}}
        """.trimIndent()
    }

    "De-Serializing - pure" {

        val result = json.decodeFromString(
            MpLocalDate.serializer(),
            """
                {"ts":1649116800000,"timezone":"UTC","human":"2022-04-05"}
            """.trimIndent()
        )

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "De-Serializing - wrapped" {

        val result = json.decodeFromString(
            Wrapper.serializer(),
            """
                {"date":{"ts":1649116800000,"timezone":"UTC","human":"2022-04-05"}}
            """.trimIndent()
        )

        result shouldBe Wrapper(
            MpLocalDate.of(2022, Month.APRIL, 5),
        )
    }
})
