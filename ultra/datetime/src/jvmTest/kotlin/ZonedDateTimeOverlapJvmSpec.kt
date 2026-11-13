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
})
