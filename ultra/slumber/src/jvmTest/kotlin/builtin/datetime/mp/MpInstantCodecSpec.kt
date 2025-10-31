package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import de.peekandpoke.ultra.slumber.slumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

class MpInstantCodecSpec : StringSpec({

    val codec = Codec.default

    data class Wrapper(val date: MpInstant)

    "Slumber - pure" {

        val subject: MpInstant = MpLocalDate.of(2022, Month.APRIL, 5)
            .atStartOfDay(TimeZone.UTC).toInstant()

        val result = codec.slumber(subject)

        result shouldBe mapOf(
            "ts" to 1649116800000L,
            "timezone" to "UTC",
            "human" to "2022-04-05T00:00:00.000Z",
        )
    }

    "Serializing - wrapped" {

        val subject = Wrapper(
            MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant(),
        )

        val result = codec.slumber(subject)

        result shouldBe mapOf(
            "date" to mapOf(
                "ts" to 1649116800000L,
                "timezone" to "UTC",
                "human" to "2022-04-05T00:00:00.000Z",
            )
        )
    }

    "De-Serializing - pure - UTC" {

        val input = mapOf(
            "ts" to 1649116800000, "timezone" to "UTC"
        )

        val result = codec.awake<MpInstant>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant()
    }

    "De-Serializing - pure - Europe/Bucharest" {

        val input = mapOf(
            "ts" to 1649116800000, "timezone" to "Europe/Bucharest"
        )

        val result = codec.awake<MpInstant>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant()
    }

    "De-Serializing - pure - Us/Pacific" {

        val input = mapOf(
            "ts" to 1649116800000, "timezone" to "Us/Pacific"
        )

        val result = codec.awake<MpInstant>(input)

        result shouldBe MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant()
    }

    "De-Serializing - wrapped" {

        val input = mapOf(
            "date" to mapOf(
                "ts" to 1649116800000L,
                "timezone" to "UTC",
                "human" to "2022-04-05T00:00:00.000Z",
            )
        )

        val result = codec.awake<Wrapper>(input)

        result shouldBe Wrapper(
            MpLocalDate.of(2022, Month.APRIL, 5).atStartOfDay(TimeZone.UTC).toInstant(),
        )
    }

    "Roundtrip - pure" {

        val start: MpInstant = MpLocalDate.of(2022, Month.APRIL, 5)
            .atStartOfDay(TimeZone.UTC).toInstant()

        val result = codec.awake<MpInstant>(
            codec.slumber(start)
        )

        result shouldBe start
    }
})
