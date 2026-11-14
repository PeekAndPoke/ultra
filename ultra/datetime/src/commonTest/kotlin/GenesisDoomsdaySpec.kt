package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

/** ULTRA-00343: Genesis = 0001-01-01T00:00:00Z, Doomsday = 9999-12-31T00:00:00Z. */
@Suppress("unused")
class GenesisDoomsdaySpec : StringSpec({

    "the timestamps" {
        GENESIS_TIMESTAMP shouldBe -62_135_596_800_000L
        DOOMSDAY_TIMESTAMP shouldBe 253_402_214_400_000L
    }

    "every type has the same two moments" {
        MpInstant.Genesis.toIsoString() shouldBe "0001-01-01T00:00:00.000Z"
        MpInstant.Doomsday.toIsoString() shouldBe "9999-12-31T00:00:00.000Z"
        MpLocalDate.Genesis shouldBe MpLocalDate.of(1, 1, 1)
        MpLocalDate.Doomsday shouldBe MpLocalDate.of(9999, 12, 31)
        MpLocalDateTime.Genesis shouldBe MpLocalDateTime.of(1, 1, 1)
        MpLocalDateTime.Doomsday shouldBe MpLocalDateTime.of(9999, 12, 31)
        MpZonedDateTime.Genesis.toInstant() shouldBe MpInstant.Genesis
        MpZonedDateTime.Doomsday.toInstant() shouldBe MpInstant.Doomsday
    }

    "forever, beginningAt and endingAt on every range type are open where they should be" {
        val instant = MpInstant.parse("2024-07-01T10:00:00Z")
        val date = MpLocalDate.of(2024, 7, 1)
        val zoned = instant.atZone(MpTimezone.of("Europe/Berlin"))

        listOf(
            MpInstantRange.forever.let { it.hasStart to it.hasEnd },
            MpLocalDateRange.forever.let { it.hasStart to it.hasEnd },
            MpClosedLocalDateRange.forever.let { it.hasStart to it.hasEnd },
            MpZonedDateTimeRange.forever.let { it.hasStart to it.hasEnd },
        ) shouldBe List(4) { false to false }

        listOf(
            MpInstantRange.beginningAt(instant).let { it.hasStart to it.hasEnd },
            MpLocalDateRange.beginningAt(date).let { it.hasStart to it.hasEnd },
            MpClosedLocalDateRange.beginningAt(date).let { it.hasStart to it.hasEnd },
            MpZonedDateTimeRange.beginningAt(zoned).let { it.hasStart to it.hasEnd },
        ) shouldBe List(4) { true to false }

        listOf(
            MpInstantRange.endingAt(instant).let { it.hasStart to it.hasEnd },
            MpLocalDateRange.endingAt(date).let { it.hasStart to it.hasEnd },
            MpClosedLocalDateRange.endingAt(date).let { it.hasStart to it.hasEnd },
            MpZonedDateTimeRange.endingAt(zoned).let { it.hasStart to it.hasEnd },
        ) shouldBe List(4) { false to true }
    }

    "ranges stored with the OLD sentinels (-10000-01-01, +10000-01-01) still read as open (kotlinx)" {
        val oldGenesis = """{"ts":-377736739200000,"timezone":"UTC","human":""}"""
        val oldDoomsday = """{"ts":253402300800000,"timezone":"UTC","human":""}"""
        val stored = """{"from":$oldGenesis,"to":$oldDoomsday}"""

        val instants = Json.decodeFromString(MpInstantRange.serializer(), stored)
        val dates = Json.decodeFromString(MpLocalDateRange.serializer(), stored)
        val zoned = Json.decodeFromString(MpZonedDateTimeRange.serializer(), stored)

        listOf(instants.hasStart, instants.hasEnd, dates.hasStart, dates.hasEnd, zoned.hasStart, zoned.hasEnd) shouldBe
                List(6) { false }
        dates.to shouldBe MpLocalDate.of(10000, 1, 1)
    }

    "dates outside Genesis to Doomsday stay valid plain values" {
        MpLocalDate.of(-500, 3, 15).year shouldBe -500
        MpLocalDate.of(12000, 1, 1).plusDays(1) shouldBe MpLocalDate.of(12000, 1, 2)
        MpLocalDateTime.parse("-10000-01-01T00:00:00Z") shouldBe MpLocalDateTime.of(-10000, 1, 1)
        MpLocalDateTime.parse("+10000-01-01T00:00:00Z") shouldBe MpLocalDateTime.of(10000, 1, 1)

        // a range that starts before year 1 counts as open at its start (accepted)
        MpLocalDateRange(MpLocalDate.of(-500, 1, 1), MpLocalDate.of(2024, 1, 1)).hasStart shouldBe false
    }

    "numberOfDays counts a range that reaches past Doomsday only up to Doomsday (the documented clamp)" {
        val range = MpLocalDateRange(MpLocalDate.of(2024, 1, 1), MpLocalDate.of(12000, 1, 1))

        range.hasEnd shouldBe false
        range.numberOfDays shouldBe 2_913_173 // 2024-01-01 to 9999-12-30, i.e. up to Doomsday
        MpLocalDateRange.forever.numberOfDays shouldBe 3_652_058

        // pinned as found: the clamp cuts the last noon to Doomsday's 00:00, and rounding half to even then counts
        // Doomsday itself for every second start date (pre-existing, reported in ULTRA-00343)
        MpLocalDateRange(MpLocalDate.of(2024, 1, 2), MpLocalDate.of(12000, 1, 1)).numberOfDays shouldBe 2_913_173
    }

    "Genesis seen in a zone west of UTC falls on 0000-12-31 (accepted)" {
        MpInstant.Genesis.atZone(MpTimezone.of("America/New_York")).toLocalDate() shouldBe MpLocalDate.of(0, 12, 31)
    }

    "DateTimeRangeConverter clamps a range past the sentinels to them" {
        val utc = MpTimezone.UTC
        val range = DateTimeRangeConverter(utc, MpLocalDate.of(-500, 1, 1), MpLocalDate.of(12000, 1, 1))
            .fromHourToHour(fromHour = 12, toHour = 12)

        range.from shouldBe MpZonedDateTime.Genesis
        range.to shouldBe MpZonedDateTime.Doomsday
    }
})
