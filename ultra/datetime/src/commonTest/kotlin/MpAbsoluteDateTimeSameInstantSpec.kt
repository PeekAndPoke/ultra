package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Duration.Companion.seconds

/** ULTRA-00211: equality is instant + zone; `isSameInstant` compares the point in time only. */
@Suppress("unused")
class MpAbsoluteDateTimeSameInstantSpec : StringSpec({

    val berlin = MpTimezone.of("Europe/Berlin")
    val london = MpTimezone.of("Europe/London")

    "09:00 Berlin and 08:00 London at the same instant are the same instant, but not equal" {
        val instant = MpInstant.parse("2022-07-01T07:00:00Z")
        val inBerlin = instant.atZone(berlin)
        val inLondon = instant.atZone(london)

        inBerlin.hour shouldBe 9
        inLondon.hour shouldBe 8

        inBerlin.isSameInstant(inLondon) shouldBe true
        inLondon.isSameInstant(inBerlin) shouldBe true
        inBerlin shouldNotBe inLondon
    }

    "the two sides of the Berlin 2022-10-30 overlap are not the same instant" {
        val first = MpInstant.fromEpochMillis(1_667_089_800_000L).atZone(berlin) // 02:30+02:00
        val second = MpInstant.fromEpochMillis(1_667_093_400_000L).atZone(berlin) // 02:30+01:00

        first.isSameInstant(second) shouldBe false
        second.isSameInstant(first) shouldBe false
    }

    "an MpInstant and an MpZonedDateTime at that instant, both directions" {
        val instant = MpInstant.parse("2022-07-01T07:00:00Z")
        val zoned = instant.atZone(berlin)

        instant.isSameInstant(zoned) shouldBe true
        zoned.isSameInstant(instant) shouldBe true

        instant.isSameInstant(zoned.plus(1.seconds)) shouldBe false
    }

    "a value against itself" {
        val instant = MpInstant.parse("2022-07-01T07:00:00Z")
        val zoned = instant.atZone(berlin)

        instant.isSameInstant(instant) shouldBe true
        zoned.isSameInstant(zoned) shouldBe true
    }
})
