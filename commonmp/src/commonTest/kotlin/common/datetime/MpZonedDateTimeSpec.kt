package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

@Suppress("unused")
class MpZonedDateTimeSpec : StringSpec({

    "Construction - parse(isoString, timezone)" {

        val bucharest = MpZonedDateTime.parse("2022-04-05T03:00", TimeZone.of("Europe/Bucharest"))
        val berlin = MpZonedDateTime.parse("2022-04-05T02:00", TimeZone.of("Europe/Berlin"))
        val utc = MpZonedDateTime.parse("2022-04-05T00:00", TimeZone.of("UTC"))

        bucharest.toEpochMillis() shouldBe utc.toEpochMillis()
        berlin.toEpochMillis() shouldBe utc.toEpochMillis()
    }

    "Construction - parse(isoString)" {

        val utc = MpZonedDateTime.parse("2022-04-05T00:00:00.000", TimeZone.UTC)

        val parsedUtcWithZ = MpZonedDateTime.parse("2022-04-05T00:00:00.000Z")

        parsedUtcWithZ.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtcWithZ.timezone shouldBe TimeZone.UTC

        val parsedUtc = MpZonedDateTime.parse("2022-04-05T00:00:00.000[UTC]")

        parsedUtc.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedUtc.timezone shouldBe TimeZone.UTC

        val parsedEuropeBucharest = MpZonedDateTime.parse("2022-04-05T03:00:00.000[Europe/Bucharest]")

        parsedEuropeBucharest.toEpochMillis() shouldBe utc.toEpochMillis()
        parsedEuropeBucharest.timezone shouldBe TimeZone.of("Europe/Bucharest")
    }

    "toString" {

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.UTC).toString() shouldBe
                "2022-04-05T00:00Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("UTC")).toString() shouldBe
                "2022-04-05T00:00Z"

        MpZonedDateTime.parse("2022-04-05T00:00:00", TimeZone.of("Europe/Berlin")).toString() shouldBe
                "2022-04-05T00:00[Europe/Berlin]"
    }

    "Equality" {

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeEqualComparingTo
                MpZonedDateTime.parse("2022-04-05T00:00:00Z")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeLessThan
                MpZonedDateTime.parse("2022-04-05T00:00:01Z")

        MpZonedDateTime.parse("2022-04-05T00:00:01Z") shouldBeGreaterThan
                MpZonedDateTime.parse("2022-04-05T00:00:00Z")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeEqualComparingTo
                MpZonedDateTime.parse("2022-04-05T03:00:00[Europe/Bucharest]")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeGreaterThan
                MpZonedDateTime.parse("2022-04-05T00:00:00[Europe/Bucharest]")

        MpZonedDateTime.parse("2022-04-05T00:00:00Z") shouldBeLessThan
                MpZonedDateTime.parse("2022-04-05T00:00:00[US/Pacific]")
    }
})
