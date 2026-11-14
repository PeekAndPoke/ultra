package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.sign

/**
 * ULTRA-00211 (maintainer, 2026-11-14): `compareTo` orders by instant, then local date-time, then zone id, like
 * java.time's `ChronoZonedDateTime`, so `compareTo == 0` exactly when `equals`.
 */
@Suppress("unused")
class MpZonedDateTimeCompareSpec : StringSpec({

    val utc = MpTimezone.UTC
    val berlin = MpTimezone.of("Europe/Berlin")
    val paris = MpTimezone.of("Europe/Paris")
    val instant = MpInstant.parse("2022-07-01T07:00:00Z")

    "one instant in UTC and in Berlin: not 0, antisymmetric, the earlier local time first" {
        val inUtc = instant.atZone(utc) // 07:00
        val inBerlin = instant.atZone(berlin) // 09:00

        inUtc.compareTo(inBerlin) shouldNotBe 0
        inUtc.compareTo(inBerlin).sign shouldBe -inBerlin.compareTo(inUtc).sign
        (inUtc < inBerlin) shouldBe true
        inUtc.isSameInstant(inBerlin) shouldBe true
    }

    "the same instant and the same offset (Berlin, Paris): ordered by zone id" {
        val inBerlin = instant.atZone(berlin)
        val inParis = instant.atZone(paris)

        inBerlin.datetime shouldBe inParis.datetime
        (inBerlin < inParis) shouldBe true
        (inParis > inBerlin) shouldBe true
    }

    "a sort of mixed zones is by instant first, then local time, then zone id" {
        val later = MpInstant.parse("2022-07-01T08:00:00Z")

        val sorted = listOf(
            later.atZone(utc),
            instant.atZone(paris),
            later.atZone(berlin),
            instant.atZone(utc),
            instant.atZone(berlin),
        ).sorted()

        sorted shouldBe listOf(
            instant.atZone(utc),
            instant.atZone(berlin),
            instant.atZone(paris),
            later.atZone(utc),
            later.atZone(berlin),
        )
    }

    "compareTo == 0 exactly when equals, over a table" {
        val first = MpInstant.fromEpochMillis(1_667_089_800_000L).atZone(berlin) // overlap, 02:30+02:00
        val second = MpInstant.fromEpochMillis(1_667_093_400_000L).atZone(berlin) // overlap, 02:30+01:00

        val values = listOf(
            instant.atZone(utc), instant.atZone(berlin), instant.atZone(paris), first, second,
            MpZonedDateTime.of(MpLocalDateTime.of(2022, 7, 1, 9, 0), berlin), // equal to instant.atZone(berlin)
        )

        values.forEach { a ->
            values.forEach { b ->
                (a.compareTo(b) == 0) shouldBe (a == b)
            }
        }
    }
})
