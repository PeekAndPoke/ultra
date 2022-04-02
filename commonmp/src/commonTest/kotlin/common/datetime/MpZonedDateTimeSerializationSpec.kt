package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.datetime.TestConstants.tsUtc_20220405_121314
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
            {"ts":1649160794000,"timezone":"Z","human":"2022-04-05T12:13:14.000Z"}
        """.trimIndent()
    }

    "Serializing - pure - Europe/Bucharest" {

        val result = json.encodeToString(
            MpZonedDateTime.serializer(),
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("Europe/Bucharest")),
        )

        result shouldBe """
            {"ts":1649149994000,"timezone":"Europe/Bucharest","human":"2022-04-05T12:13:14.000[Europe/Bucharest]"}
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
            {"date":{"ts":1649149994000,"timezone":"Europe/Bucharest","human":"2022-04-05T12:13:14.000[Europe/Bucharest]"}}
        """.trimIndent()
    }

    "De-Serializing - pure - UTC" {

        val timezone = TimeZone.of("Z")

        val result = json.decodeFromString(
            MpZonedDateTime.serializer(),
            """
                {"ts":$tsUtc_20220405_121314,"timezone":"${timezone.id}"}
            """.trimIndent()
        )

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14).atZone(timezone)

        result.toEpochMillis() shouldBe tsUtc_20220405_121314
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val timezone = TimeZone.of("Europe/Bucharest")

        val result = json.decodeFromString(
            MpZonedDateTime.serializer(),
            """
                {"ts":$tsUtc_20220405_121314,"timezone":"${timezone.id}"}
            """.trimIndent()
        )

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 15, 13, 14).atZone(timezone)

        result.toEpochMillis() shouldBe tsUtc_20220405_121314
    }

    "De-Serializing - pure - US/Pacific" {

        val timezone = TimeZone.of("US/Pacific")

        val result = json.decodeFromString(
            MpZonedDateTime.serializer(),
            """
                {"ts":$tsUtc_20220405_121314,"timezone":"${timezone.id}"}
            """.trimIndent()
        )

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 5, 13, 14).atZone(timezone)

        result.toEpochMillis() shouldBe tsUtc_20220405_121314
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

    "Roundtrip - pure" {

        val start: MpZonedDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5)
            .atZone(TimeZone.of("Europe/Bucharest"))

        val result: MpZonedDateTime = json.decodeFromString(
            MpZonedDateTime.serializer(),
            json.encodeToString(
                MpZonedDateTime.serializer(),
                start
            )
        )

        result shouldBe start
    }
})
