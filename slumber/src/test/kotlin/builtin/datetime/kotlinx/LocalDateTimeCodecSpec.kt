package de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import de.peekandpoke.ultra.slumber.Codec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class LocalDateTimeCodecSpec : StringSpec({

    "Awaking a LocalDateTime without a timezone" {

        val codec = Codec.default

        // UTC 2022-03-31T23:00:00.000Z
        val instant = Instant.fromEpochMilliseconds(1648767600_000)

        val data = mapOf(
            "ts" to instant.toEpochMilliseconds()
        )

        val result = codec.awake(LocalDateTime::class, data)!!

        result shouldBe instant.toLocalDateTime(TimeZone.UTC)

        result.toString() shouldBe "2022-03-31T23:00"
    }

    "Awaking a LocalDateTime with the timezone 'Europe/Bucharest'" {

        val codec = Codec.default

        // UTC 2022-03-31T23:00:00.000Z
        val instant = Instant.fromEpochMilliseconds(1648767600_000)

        val data = mapOf(
            "ts" to instant.toEpochMilliseconds(),
            "timezone" to "Europe/Bucharest",
        )

        val result = codec.awake(LocalDateTime::class, data)!!

        val timezone = TimeZone.of("Europe/Bucharest")

        result shouldBe instant.toLocalDateTime(timezone)

        result.toInstant(TimeZone.UTC).toEpochMilliseconds() shouldBe
                instant.toEpochMilliseconds() + timezone.offsetAt(instant).totalSeconds * 1000

        result.toString() shouldBe "2022-04-01T02:00"
    }

    "Slumbering a LocalDateTime at UTC" {

        val codec = Codec.default

        // UTC 2022-03-31T23:00:00.000Z
        val instant = Instant.fromEpochMilliseconds(1648767600_000)

        val source = instant.toLocalDateTime(TimeZone.UTC)

        val result = codec.slumber(source)

        result shouldBe mapOf(
            "ts" to instant.toEpochMilliseconds(),
            "timezone" to "Z",
            "human" to "2022-03-31T23:00:00Z",
        )
    }

    "Slumbering a LocalDateTime at 'Europe/Bucharest'" {

        val codec = Codec.default

        // UTC 2022-03-31T23:00:00.000Z
        val instant = Instant.fromEpochMilliseconds(1648767600_000)

        val timezone = TimeZone.of("Europe/Bucharest")

        val source = instant.toLocalDateTime(timezone)

        source.toInstant(timezone) shouldBe instant

        val result = codec.slumber(source)

        result shouldBe mapOf(
            "ts" to instant.toEpochMilliseconds() + timezone.offsetAt(instant).totalSeconds * 1000,
            "timezone" to "Z",
            "human" to "2022-04-01T02:00:00Z",
        )
    }
})
