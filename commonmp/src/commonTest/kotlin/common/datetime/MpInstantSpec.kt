package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class MpInstantSpec : StringSpec({

    "Construction" {

        MpInstant.fromEpochMillis(DOOMSDAY_TIMESTAMP) shouldBe MpInstant.Doomsday

        MpInstant.fromEpochMillis(GENESIS_TIMESTAMP) shouldBe MpInstant.Genesis

        MpInstant.fromEpochSeconds((DOOMSDAY_TIMESTAMP / 1000), 0) shouldBe
                MpInstant.Doomsday

        MpInstant.parse("2022-04-05T12:00:00Z").toEpochSeconds() shouldBe 1649160000L
    }

    "Genesis and Doomsday" {

        MpInstant.Genesis.toIsoString() shouldBe "-10000-01-01T00:00:00.000Z"

        MpInstant.Genesis.toEpochMillis() shouldBe GENESIS_TIMESTAMP

        MpInstant.Doomsday.toIsoString() shouldBe "+10000-01-01T00:00:00.000Z"

        MpInstant.Doomsday.toEpochMillis() shouldBe DOOMSDAY_TIMESTAMP
    }

    "Equality" {

        MpInstant.parse("2022-04-01T00:00:00Z") shouldBeEqualComparingTo
                MpInstant.parse("2022-04-01T00:00:00Z")

        MpInstant.parse("2022-04-01T00:00:00Z") shouldBeLessThan
                MpInstant.parse("2022-04-02T00:00:00Z")

        MpInstant.parse("2022-04-02T00:00:00Z") shouldBeGreaterThan
                MpInstant.parse("2022-04-01T00:00:00Z")
    }

    "toString" {
        MpInstant.parse("2022-04-01T00:00:00Z")
            .toString() shouldBe "MpInstant(value=2022-04-01T00:00:00Z)"
    }

    "toIsoString" {
        MpInstant.parse("2022-04-01T01:02:03Z")
            .toIsoString() shouldBe "2022-04-01T01:02:03.000Z"

        MpInstant.parse("2022-04-01T01:02:03.001Z")
            .toIsoString() shouldBe "2022-04-01T01:02:03.001Z"

        MpInstant.parse("2022-04-01T01:02:03.012Z")
            .toIsoString() shouldBe "2022-04-01T01:02:03.012Z"
    }

    "parse - toIsoString - round trip" {
        val start = MpInstant.parse("2022-04-01T00:00:00Z")

        val result = MpInstant.parse(start.toIsoString())

        start shouldBe result
    }

    "Arithmetic - plus duration" {

        val start: MpInstant = MpLocalDateTime.of(2022, Month.APRIL, 5).toInstant(TimeZone.UTC)
        val startTs = start.toEpochSeconds()

        start.plus(1.seconds) shouldBe MpInstant.fromEpochSeconds(startTs + 1)
        start.plus(1.hours) shouldBe MpInstant.fromEpochSeconds(startTs + (60 * 60))
    }

    "Arithmetic - minus duration" {

        val start: MpInstant = MpLocalDateTime.of(2022, Month.APRIL, 5).toInstant(TimeZone.UTC)
        val startTs = start.toEpochSeconds()

        start.minus(1.seconds) shouldBe MpInstant.fromEpochSeconds(startTs - 1)
        start.minus(1.hours) shouldBe MpInstant.fromEpochSeconds(startTs - (60 * 60))
    }

    "atZone - Europe/Bucharest" {

        val start: MpInstant = MpLocalDateTime.of(2022, Month.APRIL, 5).toInstant(TimeZone.UTC)

        val atZone = start.atZone(TimeZone.of("Europe/Bucharest"))

        atZone.toIsoString() shouldBe "2022-04-05T03:00:00.000[Europe/Bucharest]"
    }
})
