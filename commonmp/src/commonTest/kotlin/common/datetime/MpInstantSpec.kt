package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class MpInstantSpec : StringSpec({

    "Construction" {

        MpInstant.fromEpochMillis(TestConstants.tsUTC_20220405_000000) shouldBe
                MpInstant.parse("2022-04-05T00:00:00.000Z")

        MpInstant.fromEpochSeconds(TestConstants.tsUTC_20220405_000000 / 1_000) shouldBe
                MpInstant.parse("2022-04-05T00:00:00.000Z")

        MpInstant.fromEpochMillis(DOOMSDAY_TIMESTAMP) shouldBe MpInstant.Doomsday

        MpInstant.fromEpochMillis(GENESIS_TIMESTAMP) shouldBe MpInstant.Genesis

        MpInstant.fromEpochSeconds((DOOMSDAY_TIMESTAMP / 1000), 0) shouldBe
                MpInstant.Doomsday

        MpInstant.parse("2022-04-05T12:00:00Z").toEpochSeconds() shouldBe 1649160000L
    }

    "Epoch" {
        MpInstant.Epoch shouldBe MpInstant.parse("1970-01-01T00:00:00.000")
    }

    "Genesis and Doomsday" {

        MpInstant.Genesis.toIsoString() shouldBe "-10000-01-01T00:00:00.000Z"

        MpInstant.Genesis.toEpochMillis() shouldBe GENESIS_TIMESTAMP

        MpInstant.Doomsday.toIsoString() shouldBe "+10000-01-01T00:00:00.000Z"

        MpInstant.Doomsday.toEpochMillis() shouldBe DOOMSDAY_TIMESTAMP
    }

    "Equality" {
        MpInstant.parse("2022-04-01T00:00:00Z") shouldBe
                MpInstant.parse("2022-04-01T00:00:00Z")

        MpInstant.parse("2022-04-01T00:00:00Z") shouldNotBe
                MpInstant.parse("2022-04-01T00:00:00.001Z")
    }

    "Comparison" {

        MpInstant.parse("2022-04-01T00:00:00Z") shouldBeEqualComparingTo
                MpInstant.parse("2022-04-01T00:00:00Z")

        MpInstant.parse("2022-04-01T00:00:00Z") shouldBeLessThan
                MpInstant.parse("2022-04-02T00:00:00Z")

        MpInstant.parse("2022-04-02T00:00:00Z") shouldBeGreaterThan
                MpInstant.parse("2022-04-01T00:00:00Z")
    }

    "toString" {
        MpInstant.parse("2022-04-01T00:00:00Z")
            .toString() shouldBe "MpInstant(2022-04-01T00:00:00.000Z)"
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

    "atZone - Europe/Bucharest" {

        val start: MpInstant = MpLocalDateTime.of(2022, Month.APRIL, 5).toInstant(TimeZone.UTC)

        val atZone = start.atZone(TimeZone.of("Europe/Bucharest"))

        atZone.toIsoString() shouldBe "2022-04-05T03:00:00.000[Europe/Bucharest]"
    }

    "toRange" {
        val result1 = MpInstant.parse("2022-04-01T12:13:14.123Z").toRange(2.days)

        result1 shouldBe MpInstantRange(
            from = MpInstant.parse("2022-04-01T12:13:14.123Z"),
            to = MpInstant.parse("2022-04-03T12:13:14.123Z"),
        )

        val result2 = MpInstant.parse("2022-04-03T12:13:14.123Z").toRange((-2).days)

        result2 shouldBe MpInstantRange(
            from = MpInstant.parse("2022-04-03T12:13:14.123Z"),
            to = MpInstant.parse("2022-04-01T12:13:14.123Z"),
        )
    }

    "plus(duration)" {
        val now = MpInstant.now()
        val result = now.plus(1.minutes)

        result.toEpochMillis() shouldBe now.toEpochMillis() + (60 * 1_000)
    }

    "plus(value, unit, timezone)" {

        val beforeDST = MpInstant.parse("2022-03-27T00:00:00.000[Europe/Berlin]")

        val afterDST = beforeDST.plus(1, DateTimeUnit.DAY, TimeZone.of("Europe/Berlin"))

        afterDST shouldBe MpInstant.parse("2022-03-28T00:00:00.000[Europe/Berlin]")

        (afterDST - beforeDST) shouldBe 23.hours
    }

    "minus(duration)" {
        val now = MpInstant.now()
        val result = now.minus(1.minutes)

        result.toEpochMillis() shouldBe now.toEpochMillis() - (60 * 1_000)
    }

    "minus(value, unit, timezone)" {

        val beforeDST = MpInstant.parse("2022-03-28T00:00:00.000[Europe/Berlin]")

        val afterDST = beforeDST.minus(1, DateTimeUnit.DAY, TimeZone.of("Europe/Berlin"))

        afterDST shouldBe MpInstant.parse("2022-03-27T00:00:00.000[Europe/Berlin]")

        (afterDST - beforeDST) shouldBe (-23).hours
    }

    "minus(other: MpInstant)" {
        val result = MpInstant.parse("2022-04-01T01:00:00Z") - MpInstant.parse("2022-04-01T00:00:00Z")

        result shouldBe 1.hours
    }
})
