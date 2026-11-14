package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.DayOfWeek
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

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
        val closed = Json.decodeFromString(MpClosedLocalDateRange.serializer(), stored)
        val zoned = Json.decodeFromString(MpZonedDateTimeRange.serializer(), stored)

        listOf(
            instants.hasStart, instants.hasEnd, dates.hasStart, dates.hasEnd,
            closed.hasStart, closed.hasEnd, zoned.hasStart, zoned.hasEnd,
        ) shouldBe List(8) { false }
        dates.to shouldBe MpLocalDate.of(10000, 1, 1)
        closed.to shouldBe MpLocalDate.of(10000, 1, 1)
    }

    "the open-end predicates at the neighbours of the sentinels" {
        val x = MpLocalDate.of(2024, 1, 1)
        val i = MpInstant.parse("2024-01-01T00:00:00Z")
        val oneMs = 1.milliseconds

        MpLocalDateRange(x, MpLocalDate.of(9999, 12, 30)).hasEnd shouldBe true
        MpLocalDateRange(x, MpLocalDate.of(9999, 12, 31)).hasEnd shouldBe false
        MpLocalDateRange(MpLocalDate.of(1, 1, 2), x).hasStart shouldBe true
        MpLocalDateRange(MpLocalDate.of(1, 1, 1), x).hasStart shouldBe false

        MpInstantRange(i, MpInstant.Doomsday - oneMs).hasEnd shouldBe true
        MpInstantRange(i, MpInstant.Doomsday).hasEnd shouldBe false
        MpInstantRange(MpInstant.Genesis + oneMs, i).hasStart shouldBe true
        MpInstantRange(MpInstant.Genesis, i).hasStart shouldBe false

        // zoned: by instant, in a zone that is not UTC
        val berlin = MpTimezone.of("Europe/Berlin")
        val z = i.atZone(berlin)
        MpZonedDateTimeRange(z, (MpInstant.Doomsday - oneMs).atZone(berlin)).hasEnd shouldBe true
        MpZonedDateTimeRange(z, MpInstant.Doomsday.atZone(berlin)).hasEnd shouldBe false
        MpZonedDateTimeRange((MpInstant.Genesis + oneMs).atZone(berlin), z).hasStart shouldBe true
        MpZonedDateTimeRange(MpInstant.Genesis.atZone(berlin), z).hasStart shouldBe false

        // through asOpenRange (to + 1 day): today's behaviour, pinned as it is
        MpClosedLocalDateRange(x, MpLocalDate.of(9999, 12, 29)).hasEnd shouldBe true
        MpClosedLocalDateRange(x, MpLocalDate.of(9999, 12, 30)).hasEnd shouldBe false
        MpClosedLocalDateRange(MpLocalDate.of(1, 1, 2), x).hasStart shouldBe true
        MpClosedLocalDateRange(MpLocalDate.of(1, 1, 1), x).hasStart shouldBe false
    }

    "the end is open at or after 9999-12-31T00:00Z, so forever no longer contains the last day" {
        val i = MpInstant.parse("2024-01-01T00:00:00Z")

        // another system's maximum reads as "no end"
        MpInstantRange(i, MpInstant.parse("9999-12-31T23:59:59.999Z")).hasEnd shouldBe false

        MpLocalDateRange.forever.contains(MpLocalDate.of(9999, 12, 31)) shouldBe false
        MpLocalDateRange.forever.contains(MpLocalDate.of(9999, 12, 30)) shouldBe true
        MpInstantRange.forever.contains(MpInstant.parse("9999-12-31T12:00:00Z")) shouldBe false
        MpInstantRange.forever.contains(MpInstant.parse("9999-12-30T12:00:00Z")) shouldBe true
    }

    "MpClosedLocalDateRange.beginningAt(d).asOpenRange stores to = 10000-01-01, the old Doomsday" {
        val open = MpClosedLocalDateRange.beginningAt(MpLocalDate.of(2024, 1, 1)).asOpenRange

        open.to shouldBe MpLocalDate.of(10000, 1, 1)
        open.hasEnd shouldBe false
        Json.encodeToString(MpLocalDateRange.serializer(), open) shouldContain "\"ts\":253402300800000"
    }

    "old and new sentinels mixed: cutting a new beginningAt out of an old one leaves an open-ended sliver" {
        val old = MpLocalDateRange(MpLocalDate.of(2024, 1, 1), MpLocalDate.of(10000, 1, 1))
        val pieces = old.cutAway(MpLocalDateRange.beginningAt(MpLocalDate.of(2025, 1, 1)))

        pieces shouldBe listOf(
            MpLocalDateRange(MpLocalDate.of(2024, 1, 1), MpLocalDate.of(2025, 1, 1)),
            MpLocalDateRange(MpLocalDate.of(9999, 12, 31), MpLocalDate.of(10000, 1, 1)),
        )
        pieces.map { it.hasEnd } shouldBe listOf(true, false)
    }

    "dates outside Genesis to Doomsday stay valid plain values" {
        MpLocalDate.of(-500, 3, 15).year shouldBe -500
        MpLocalDate.of(12000, 1, 1).plusDays(1) shouldBe MpLocalDate.of(12000, 1, 2)
        MpLocalDateTime.parse("-10000-01-01T00:00:00Z") shouldBe MpLocalDateTime.of(-10000, 1, 1)
        MpLocalDateTime.parse("+10000-01-01T00:00:00Z") shouldBe MpLocalDateTime.of(10000, 1, 1)

        // numDaysInMonth from the calendar; literals from an independent count (400-year cycle: -500-03 = 1100-03)
        MpLocalDate.of(-500, 3, 15).numDaysInMonth shouldBe 31
        MpLocalDate.of(0, 12, 31).numDaysInMonth shouldBe 31
        MpLocalDate.of(0, 6, 15).numDaysInMonth shouldBe 30
        MpLocalDate.of(0, 2, 1).numDaysInMonth shouldBe 29
        MpLocalDate.of(1, 2, 1).numDaysInMonth shouldBe 28
        MpLocalDate.of(9999, 12, 1).numDaysInMonth shouldBe 31
        MpLocalDate.of(10000, 1, 1).numDaysInMonth shouldBe 31
        MpLocalDate.of(12000, 2, 1).numDaysInMonth shouldBe 29

        MpLocalDate.of(0, 12, 31).getDatesInMonth(listOf(1, 15, 31)) shouldBe listOf(
            MpLocalDate.of(0, 12, 1), MpLocalDate.of(0, 12, 15), MpLocalDate.of(0, 12, 31),
        )
        MpLocalDate.of(-500, 3, 15).getDatesInMonth(listOf(1, 31, 32)) shouldBe listOf(
            MpLocalDate.of(-500, 3, 1), MpLocalDate.of(-500, 3, 31),
        )
        MpLocalDate.of(-500, 3, 15).getDatesInMonth(listOf(MpDayOfWeek.MONDAY)) shouldBe
                listOf(5, 12, 19, 26).map { MpLocalDate.of(-500, 3, it) }
        MpLocalDate.of(0, 12, 31).getDatesInMonth(listOf(DayOfWeek.MONDAY)) shouldBe
                listOf(4, 11, 18, 25).map { MpLocalDate.of(0, 12, it) }
        MpLocalDate.of(10000, 1, 1).getDatesInMonth(listOf(MpDayOfWeek.MONDAY)) shouldBe
                listOf(3, 10, 17, 24, 31).map { MpLocalDate.of(10000, 1, it) }

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
