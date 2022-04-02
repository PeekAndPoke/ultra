package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month

class MpLocalDateCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val date: MpLocalDate)

    "Serializing - pure" {

        val input: MpLocalDate = MpLocalDate.of(2022, Month.APRIL, 5)

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "ts" to 1649116800000, "timezone" to "UTC", "human" to "2022-04-05T00:00:00.000Z"
        )
    }

    "Serializing - wrapped" {

        val input = Wrapper(
            MpLocalDate.of(2022, Month.APRIL, 5),
        )

        val result = codec.slumber(input)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 1649116800000, "timezone" to "UTC", "human" to "2022-04-05T00:00:00.000Z"
            )
        )
    }

    "De-Serializing - pure - UTC" {

        val input = mapOf(
            "ts" to 1649116800000L, "timezone" to "UTC"
        )

        val result = codec.awake<MpLocalDate>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val input = mapOf(
            "ts" to 1649116800000L, "timezone" to "Europe/Bucharest"
        )

        val result = codec.awake<MpLocalDate>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5)
    }

    "De-Serializing - pure - US/Pacific" {

        val input = mapOf(
            "ts" to 1649116800000L, "timezone" to "US/Pacific"
        )

        val result = codec.awake<MpLocalDate>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 4)
    }

    "De-Serializing - wrapped" {

        val input = mapOf(
            "date" to mapOf(
                "ts" to 1649116800000, "timezone" to "UTC", "human" to "2022-04-05T00:00:00.000Z"
            )
        )

        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(
            MpLocalDate.of(2022, Month.APRIL, 5),
        )
    }

    "Roundtrip - pure" {

        val start: MpLocalDate = MpLocalDate.of(2022, Month.APRIL, 5)

        val result = codec.awake<MpLocalDate>(
            codec.slumber(start)
        )

        result shouldBe start
    }
})
