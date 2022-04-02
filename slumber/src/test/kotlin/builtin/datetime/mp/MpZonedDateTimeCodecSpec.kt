package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

class MpZonedDateTimeCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val date: MpZonedDateTime)

    "Serializing - pure - UTC" {

        val input: MpZonedDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
            .atZone(TimeZone.of("UTC"))

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "ts" to 1649160794000L,
            "timezone" to "Z",
            "human" to "2022-04-05T12:13:14.000Z",
        )
    }

    "Serializing - pure - Europe/Bucharest" {

        val timezone = TimeZone.of("Europe/Bucharest")

        val input: MpZonedDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
            .atZone(timezone)

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "ts" to 1649149994000L,
            "timezone" to timezone.id,
            "human" to "2022-04-05T12:13:14.000[Europe/Bucharest]",
        )
    }

    "Serializing - pure - US/Pacific" {

        val timezone = TimeZone.of("US/Pacific")

        val input: MpZonedDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
            .atZone(timezone)

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "ts" to 1649185994000L,
            "timezone" to timezone.id,
            "human" to "2022-04-05T12:13:14.000[US/Pacific]",
        )
    }

    "Serializing - wrapped - Europe/Bucharest" {

        val input = Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("Europe/Bucharest")),
        )

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 1649149994000,
                "timezone" to "Europe/Bucharest",
                "human" to "2022-04-05T12:13:14.000[Europe/Bucharest]",
            )
        )
    }

    "De-Serializing - pure - UTC" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.of("Z")

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpZonedDateTime>(input)

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14).atZone(timezone)

        result!!.toEpochMillis() shouldBe timestamp
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.of("Europe/Bucharest")

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpZonedDateTime>(input)

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 15, 13, 14).atZone(timezone)

        result!!.toEpochMillis() shouldBe timestamp
    }

    "De-Serializing - pure - US/Pacific" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.of("US/Pacific")

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpZonedDateTime>(input)

        result shouldBe
                MpLocalDateTime.of(2022, Month.APRIL, 5, 5, 13, 14).atZone(timezone)

        result!!.toEpochMillis() shouldBe timestamp
    }

    "De-Serializing - wrapped - Europe/Bucharest" {

        val input = mapOf(
            "date" to mapOf(
                "ts" to 1649149994000,
                "timezone" to "Europe/Bucharest",
                "human" to "2022-04-05T12:13:14.000[Europe/Bucharest]",
            )
        )

        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)
                .atZone(TimeZone.of("Europe/Bucharest")),
        )
    }

    "Roundtrip - pure" {

        val start: MpZonedDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5)
            .atZone(TimeZone.of("Europe/Bucharest"))

        val result = codec.awake<MpZonedDateTime>(
            codec.slumber(start)
        )

        result shouldBe start
    }
})
