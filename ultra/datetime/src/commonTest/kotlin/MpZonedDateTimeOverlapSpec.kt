package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

/**
 * ULTRA-00211: the instant is the truth in [MpZonedDateTime]. Both sides of a DST overlap and a time in a gap survive
 * every conversion and the kotlinx serializer with their instant.
 */
@Suppress("unused")
class MpZonedDateTimeOverlapSpec : StringSpec({

    val berlin = MpTimezone.of("Europe/Berlin")

    // 2022-10-30, Berlin falls back from 03:00+02:00 to 02:00+01:00: 02:30 happens twice
    val firstHalf = 1_667_089_800_000L // 02:30+02:00 = 00:30Z
    val secondHalf = 1_667_093_400_000L // 02:30+01:00 = 01:30Z

    // 2022-03-27, Berlin springs forward from 02:00+01:00 to 03:00+02:00: 02:30 does not exist
    val gapLocal = MpLocalDateTime.of(2022, 3, 27, 2, 30)
    val gapResolved = 1_648_344_600_000L // 03:30+02:00 = 01:30Z

    listOf("first half" to firstHalf, "second half" to secondHalf).forEach { (side, ts) ->

        "overlap, $side: atZone keeps the instant" {
            MpInstant.fromEpochMillis(ts).atZone(berlin).toEpochMillis() shouldBe ts
        }

        "overlap, $side: the local fields read 02:30" {
            val subject = MpInstant.fromEpochMillis(ts).atZone(berlin)

            subject.datetime shouldBe MpLocalDateTime.of(2022, 10, 30, 2, 30)
        }

        "overlap, $side: a kotlinx Json round trip gives back the same instant and an equal value" {
            val subject = MpInstant.fromEpochMillis(ts).atZone(berlin)

            val decoded = Json.decodeFromString(
                MpZonedDateTimeSerializer,
                Json.encodeToString(MpZonedDateTimeSerializer, subject),
            )

            decoded.toEpochMillis() shouldBe ts
            decoded shouldBe subject
        }
    }

    "overlap: the two sides are different values, ordered by their instant" {
        val first = MpInstant.fromEpochMillis(firstHalf).atZone(berlin)
        val second = MpInstant.fromEpochMillis(secondHalf).atZone(berlin)

        first shouldNotBe second
        (first < second) shouldBe true
    }

    "overlap: one hour after the first half is the second half" {
        val first = MpInstant.fromEpochMillis(firstHalf).atZone(berlin)

        first.plus(1.hours).toEpochMillis() shouldBe secondHalf
        first.plus(1, DateTimeUnit.HOUR).toEpochMillis() shouldBe secondHalf
    }

    "overlap: the second half truncated to its hour stays in its own half" {
        val second = MpInstant.fromEpochMillis(secondHalf).atZone(berlin)

        second.atStartOfHour().toEpochMillis() shouldBe secondHalf - 30 * 60_000L // 02:00+01:00
    }

    "overlap: the second half keeps its offset through atStartOfMinute, atStartOfSecond and atTime" {
        val second = MpInstant.fromEpochMillis(secondHalf + 15_500L).atZone(berlin) // 02:30:15.500+01:00

        second.atStartOfMinute().toEpochMillis() shouldBe secondHalf
        second.atStartOfSecond().toEpochMillis() shouldBe secondHalf + 15_000L
        second.atTime(MpLocalTime.of(2, 10)).toEpochMillis() shouldBe secondHalf - 20 * 60_000L // 02:10+01:00
    }

    // equality is decided in one place, MpZonedDateTime.equals / hashCode (ULTRA-00211, confirmed 2026-11-14)
    "equality: instant and zone decide; equal values have equal hash codes" {
        val first = MpInstant.fromEpochMillis(firstHalf).atZone(berlin)
        val second = MpInstant.fromEpochMillis(secondHalf).atZone(berlin)
        val gapBuilt = MpZonedDateTime.of(gapLocal, berlin)
        val gapAtInstant = MpInstant.fromEpochMillis(gapResolved).atZone(berlin)

        first shouldNotBe second
        gapBuilt shouldBe gapAtInstant
        gapBuilt.hashCode() shouldBe gapAtInstant.hashCode()

        // the same instant in another zone is another value
        MpInstant.fromEpochMillis(firstHalf).atZone(MpTimezone.UTC) shouldNotBe first
    }

    "overlap: toString shows the offset, so the two sides print differently; toIsoString does not change" {
        val first = MpInstant.fromEpochMillis(firstHalf).atZone(berlin)
        val second = MpInstant.fromEpochMillis(secondHalf).atZone(berlin)

        first.toString() shouldBe "MpZonedDateTime(2022-10-30T02:30:00.000+02:00[Europe/Berlin])"
        second.toString() shouldBe "MpZonedDateTime(2022-10-30T02:30:00.000+01:00[Europe/Berlin])"

        first.toIsoString() shouldBe "2022-10-30T02:30:00.000[Europe/Berlin]"
        second.toIsoString() shouldBe "2022-10-30T02:30:00.000[Europe/Berlin]"
    }

    "toString: UTC keeps the Z, an offset with seconds shows them (Berlin local mean time 1890, +00:53:28)" {
        MpInstant.fromEpochMillis(firstHalf).atZone(MpTimezone.UTC).toString() shouldBe
                "MpZonedDateTime(2022-10-30T00:30:00.000Z)"

        MpZonedDateTime.of(MpLocalDateTime.of(1890, 6, 1, 12, 0), berlin).toString() shouldBe
                "MpZonedDateTime(1890-06-01T12:00:00.000+00:53:28[Europe/Berlin])"
    }

    "toString: west of UTC the sign is minus (New York in winter, -05:00; local mean time before 1883, -04:56:02)" {
        val newYork = MpTimezone.of("America/New_York")

        MpZonedDateTime.of(MpLocalDateTime.of(2022, 1, 15, 12, 0), newYork).toString() shouldBe
                "MpZonedDateTime(2022-01-15T12:00:00.000-05:00[America/New_York])"

        MpZonedDateTime.of(MpLocalDateTime.of(1880, 6, 1, 12, 0), newYork).toString() shouldBe
                "MpZonedDateTime(1880-06-01T12:00:00.000-04:56:02[America/New_York])"
    }

    "gap: a start of the hour inside a gap resolves like of, later than the receiver (Toronto 1919-03-31T00:45)" {
        val toronto = MpTimezone.of("America/Toronto")
        val subject = MpZonedDateTime.of(MpLocalDateTime.of(1919, 3, 31, 0, 45), toronto)

        subject.datetime shouldBe MpLocalDateTime.of(1919, 3, 31, 0, 45)
        subject.atStartOfHour().datetime shouldBe MpLocalDateTime.of(1919, 3, 31, 1, 0)
        (subject.atStartOfHour() > subject) shouldBe true
    }

    "overlap: of(local, zone) takes the earlier offset" {
        MpZonedDateTime.of(MpLocalDateTime.of(2022, 10, 30, 2, 30), berlin).toEpochMillis() shouldBe firstHalf
    }

    "gap: of(local, zone) moves forward by the gap, and the local fields read the resolved time" {
        val subject = MpZonedDateTime.of(gapLocal, berlin)

        subject.toEpochMillis() shouldBe gapResolved
        subject.datetime shouldBe MpLocalDateTime.of(2022, 3, 27, 3, 30)
        subject.hour shouldBe 3
    }

    "gap: a value built from the gap time equals the one at its instant" {
        MpZonedDateTime.of(gapLocal, berlin) shouldBe MpInstant.fromEpochMillis(gapResolved).atZone(berlin)
    }

    "gap: a kotlinx Json round trip gives back the same instant and an equal value" {
        val subject = MpZonedDateTime.of(gapLocal, berlin)

        val decoded = Json.decodeFromString(
            MpZonedDateTimeSerializer,
            Json.encodeToString(MpZonedDateTimeSerializer, subject),
        )

        decoded.toEpochMillis() shouldBe gapResolved
        decoded shouldBe subject
    }

    "atStartOfDay agrees with MpLocalDate.atStartOfDay (Toronto 1919-03-31, midnight in a gap)" {
        val toronto = MpTimezone.of("America/Toronto")
        val subject = MpZonedDateTime.of(MpLocalDateTime.of(1919, 3, 31, 12, 0), toronto)

        val expected = MpLocalDate.of(1919, 3, 31).atStartOfDay(toronto)

        subject.atStartOfDay().toEpochMillis() shouldBe expected.toEpochMillis()
        subject.atStartOfDay() shouldBe expected
    }
})
