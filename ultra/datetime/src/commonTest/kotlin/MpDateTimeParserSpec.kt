package de.peekandpoke.ultra.datetime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

@Suppress("unused")
class MpDateTimeParserSpec : StringSpec({

    "parseInstant with Z (UTC)" {
        val result = MpDateTimeParser.parseInstant("2024-06-15T10:30:00Z")

        val zoned = result.atZone(TimeZone.UTC)
        zoned.year shouldBe 2024
        zoned.monthNumber shouldBe 6
        zoned.dayOfMonth shouldBe 15
        zoned.hour shouldBe 10
        zoned.minute shouldBe 30
    }

    "parseInstant without timezone defaults to UTC" {
        val result = MpDateTimeParser.parseInstant("2024-06-15T10:30:00")

        val zoned = result.atZone(TimeZone.UTC)
        zoned.year shouldBe 2024
        zoned.hour shouldBe 10
    }

    "parseInstant with named timezone" {
        val result = MpDateTimeParser.parseInstant("2024-06-15T10:30:00[Europe/Berlin]")

        val zoned = result.atZone(TimeZone.of("Europe/Berlin"))
        zoned.year shouldBe 2024
        zoned.hour shouldBe 10
        zoned.minute shouldBe 30
    }

    "parseInstant with milliseconds" {
        val result = MpDateTimeParser.parseInstant("2024-06-15T10:30:00.123Z")

        val zoned = result.atZone(TimeZone.UTC)
        zoned.year shouldBe 2024
        zoned.second shouldBe 0
    }

    "parseInstant throws on invalid input" {
        shouldThrow<IllegalArgumentException> {
            MpDateTimeParser.parseInstant("not-a-date")
        }
    }

    "parseZonedDateTime with Z" {
        val result = MpDateTimeParser.parseZonedDateTime("2024-01-15T08:00:00Z")

        result.year shouldBe 2024
        result.monthNumber shouldBe 1
        result.dayOfMonth shouldBe 15
        result.hour shouldBe 8
    }

    "parseZonedDateTime with named timezone" {
        val result = MpDateTimeParser.parseZonedDateTime("2024-07-15T14:00:00[America/New_York]")

        result.year shouldBe 2024
        result.monthNumber shouldBe 7
        result.hour shouldBe 14
        result.timezone.id shouldBe "America/New_York"
    }
})
