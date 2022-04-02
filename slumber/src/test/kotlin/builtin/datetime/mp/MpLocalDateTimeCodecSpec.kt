package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

class MpLocalDateTimeCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val date: MpLocalDateTime)

    "Serializing - pure" {

        val input: MpLocalDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "ts" to 1649160794000, "timezone" to "UTC", "human" to "2022-04-05T12:13:14.000Z"
        )
    }

    "Serializing - wrapped" {

        val input = Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
        )

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 1649160794000, "timezone" to "UTC", "human" to "2022-04-05T12:13:14.000Z"
            )
        )
    }

    "De-Serializing - pure - UTC" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.UTC

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpLocalDateTime>(input)

        result shouldBe MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14)

        result!!.atZone(timezone).toEpochMillis() shouldBe timestamp
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.of("Europe/Bucharest")

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpLocalDateTime>(input)

        result shouldBe MpLocalDateTime.of(2022, Month.APRIL, 5, 15, 13, 14)

        result!!.atZone(timezone).toEpochMillis() shouldBe timestamp
    }

    "De-Serializing - pure - US/Pacific" {

        val timestamp = 1649160794000L
        val timezone = TimeZone.of("US/Pacific")

        val input = mapOf(
            "ts" to timestamp, "timezone" to timezone.id
        )

        val result = codec.awake<MpLocalDateTime>(input)

        result shouldBe MpLocalDateTime.of(2022, Month.APRIL, 5, 5, 13, 14)

        result!!.atZone(timezone).toEpochMillis() shouldBe timestamp
    }


    "De-Serializing - wrapped" {

        val input = mapOf(
            "date" to mapOf(
                "ts" to 1649160794000, "timezone" to "UTC", "human" to "2022-04-05T12:13:14.000Z"
            )
        )

        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(
            MpLocalDateTime.of(2022, Month.APRIL, 5, 12, 13, 14),
        )
    }

    "Roundtrip - pure" {

        val start: MpLocalDateTime = MpLocalDateTime.of(2022, Month.APRIL, 5)

        val result = codec.awake<MpLocalDateTime>(
            codec.slumber(start)
        )

        result shouldBe start
    }
})
