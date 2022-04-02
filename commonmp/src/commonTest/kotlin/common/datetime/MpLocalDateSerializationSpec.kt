package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.datetime.TestConstants.tsUTC_20220405_000000
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
            {"ts":1649116800000,"timezone":"UTC","human":"2022-04-05T00:00:00.000Z"}
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
            {"date":{"ts":1649116800000,"timezone":"UTC","human":"2022-04-05T00:00:00.000Z"}}
        """.trimIndent()
    }

    "De-Serializing - pure - UTC" {

        val result = json.decodeFromString(
            MpLocalDate.serializer(),
            """
                {"ts":$tsUTC_20220405_000000,"timezone":"UTC"}
            """.trimIndent()
        )

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val result = json.decodeFromString(
            MpLocalDate.serializer(),
            """
                {"ts":$tsUTC_20220405_000000,"timezone":"Europe/Bucharest"}
            """.trimIndent()
        )

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "De-Serializing - pure - US/Pacific" {

        val result = json.decodeFromString(
            MpLocalDate.serializer(),
            """
                {"ts":$tsUTC_20220405_000000,"timezone":"US/Pacific"}
            """.trimIndent()
        )

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 4)
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

    "Roundtrip - pure" {

        val start: MpLocalDate = MpLocalDate.of(2022, Month.APRIL, 5)

        val result: MpLocalDate = json.decodeFromString(
            MpLocalDate.serializer(),
            json.encodeToString(
                MpLocalDate.serializer(),
                start
            )
        )

        result shouldBe start
    }
})
