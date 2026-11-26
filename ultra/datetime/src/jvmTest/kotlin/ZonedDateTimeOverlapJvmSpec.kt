package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** ULTRA-00211: the java.time converters keep the instant on both sides of an overlap and in a gap. */
@Suppress("unused")
class ZonedDateTimeOverlapJvmSpec : StringSpec({

    val berlin = ZoneId.of("Europe/Berlin")

    listOf(
        "overlap, first half" to 1_667_089_800_000L,
        "overlap, second half" to 1_667_093_400_000L,
        "after a gap" to 1_648_344_600_000L,
    ).forEach { (case, ts) ->

        "$case: ZonedDateTime.mp keeps the instant" {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), berlin).mp.toEpochMillis() shouldBe ts
        }

        "$case: MpZonedDateTime.jvm keeps the instant" {
            val subject = MpInstant.fromEpochMillis(ts).atZone(MpTimezone.of("Europe/Berlin"))

            subject.jvm.toInstant().toEpochMilli() shouldBe ts
        }
    }

    // beyond the Long epoch-millis range (about +-292 million years), where a conversion by millis clamps or throws
    listOf(10_000_000_000_000_000L, -10_000_000_000_000_000L).forEach { seconds ->
        "epoch second $seconds: MpZonedDateTime.jvm and ZonedDateTime.mp are exact, at millisecond precision" {
            val mp = MpInstant.fromEpochSeconds(seconds, 123_000_000).atZone(MpTimezone.of("Europe/Berlin"))

            mp.jvm.toInstant() shouldBe Instant.ofEpochSecond(seconds, 123_000_000)
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, 123_456_789), berlin).mp shouldBe mp
        }
    }

    // maintainer, 2026-11-14: the converters stay at millisecond precision in both directions
    "ZonedDateTime.mp cuts to the millisecond, both overlap halves" {
        listOf(1_667_089_800L, 1_667_093_400L).forEach { seconds ->
            val java = ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, 123_456_789), berlin)

            java.mp.toInstant() shouldBe MpInstant.fromEpochSeconds(seconds, 123_000_000)
            java.mp.isSameInstant(java.toInstant().mp) shouldBe true
            java.mp.jvm shouldBe ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds, 123_000_000), berlin)
        }
    }

    "MpZonedDateTime.jvm cuts to the millisecond: three fraction digits" {
        val subject = MpInstant.fromEpochSeconds(1_667_093_400L, 123_456_789).atZone(MpTimezone.of("Europe/Berlin"))

        subject.jvm.nano shouldBe 123_000_000
        subject.jvm.toInstant() shouldBe Instant.ofEpochSecond(1_667_093_400L, 123_000_000)
    }

    "before 1970 the cut is a floor, like toEpochMilli: 1 ns before the epoch is -1 ms" {
        val java = ZonedDateTime.ofInstant(Instant.ofEpochSecond(-1, 999_999_999), berlin)

        java.mp.toEpochMillis() shouldBe -1L
        java.mp shouldBe MpInstant.fromEpochMillis(-1).atZone(MpTimezone.of("Europe/Berlin"))

        val mp = MpInstant.fromEpochSeconds(-1, 999_999_999).atZone(MpTimezone.of("Europe/Berlin"))

        mp.jvm.toInstant() shouldBe Instant.ofEpochMilli(-1)
    }
})
