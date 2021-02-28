package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId

class SerializerRoundTripSpec : StringSpec() {

    init {

        "Encoding and decoding a PortableTime must work" {

            val source = LocalTime.of(12, 30, 15).portable

            val encoded = Json.encodeToString(
                PortableTime.serializer(),
                source
            )

            encoded shouldBe 45_015_000.toString()

            val decoded = Json.decodeFromString(
                PortableTime.serializer(),
                encoded
            )

            decoded shouldBe source
        }

        "Encoding and decoding a PortableDate must work" {

            val source = LocalDate.of(2020, Month.JANUARY, 15).portable

            val encoded = Json.encodeToString(
                PortableDate.serializer(),
                source
            )

            encoded shouldBe """
                {"ts":1579046400000,"timezone":"UTC","human":"2020-01-15T00:00:00"}
            """.trimIndent()

            val decoded = Json.decodeFromString(
                PortableDate.serializer(),
                encoded
            )

            decoded shouldBe source
        }

        "Encoding and decoding a PortableDateTime must work" {

            val source = LocalDateTime.of(
                LocalDate.of(2020, Month.JANUARY, 15),
                LocalTime.of(12, 30, 59)
            ).portable

            val encoded = Json.encodeToString(
                PortableDateTime.serializer(),
                source
            )

            encoded shouldBe """
                {"ts":1579091459000,"timezone":"UTC","human":"2020-01-15T12:30:59"}
            """.trimIndent()

            val decoded = Json.decodeFromString(
                PortableDateTime.serializer(),
                encoded
            )

            decoded shouldBe source
        }

        "Encoding and decoding a PortableTimezone must work" {

            val source = ZoneId.of("UTC").portable

            val encoded = Json.encodeToString(
                PortableTimezone.serializer(),
                source
            )

            encoded shouldBe """
                "UTC"
            """.trimIndent()

            val decoded = Json.decodeFromString(
                PortableTimezone.serializer(),
                encoded
            )

            decoded shouldBe source
        }
    }
}
