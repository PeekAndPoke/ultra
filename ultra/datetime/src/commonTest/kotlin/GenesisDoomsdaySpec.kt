package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

/**
 * Genesis = -10000-01-01T00:00:00Z and Doomsday = +10000-01-01T00:00:00Z, the values of master, restored for the merge
 * of ULTRA-00341 (the choice of values is open in ULTRA-00343). Pins the values on every platform, what counts as open
 * at both edges, how they are stored, and what the documented clamp does with them.
 */
@Suppress("unused")
class GenesisDoomsdaySpec : StringSpec({

    "the timestamps" {
        GENESIS_TIMESTAMP shouldBe -377_736_739_200_000L
        DOOMSDAY_TIMESTAMP shouldBe 253_402_300_800_000L
    }

    "every type has the same two moments, and their text reads back" {
        MpInstant.Genesis.toIsoString() shouldBe "-10000-01-01T00:00:00.000Z"
        MpInstant.Doomsday.toIsoString() shouldBe "+10000-01-01T00:00:00.000Z"
        MpInstant.parse(MpInstant.Genesis.toIsoString()) shouldBe MpInstant.Genesis
        MpInstant.parse(MpInstant.Doomsday.toIsoString()) shouldBe MpInstant.Doomsday
        MpLocalDate.Genesis shouldBe MpLocalDate.of(-10000, 1, 1)
        MpLocalDate.Doomsday shouldBe MpLocalDate.of(10000, 1, 1)
        MpLocalDateTime.Genesis shouldBe MpLocalDateTime.of(-10000, 1, 1)
        MpLocalDateTime.Doomsday shouldBe MpLocalDateTime.of(10000, 1, 1)
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

    "stored: an open range is written with the two timestamps and reads back open (kotlinx)" {
        val json = Json.encodeToString(MpLocalDateRange.serializer(), MpLocalDateRange.forever)

        json shouldContain "\"ts\":-377736739200000"
        json shouldContain "\"ts\":253402300800000"

        val back = Json.decodeFromString(MpLocalDateRange.serializer(), json)
        back shouldBe MpLocalDateRange.forever
        (back.hasStart to back.hasEnd) shouldBe (false to false)

        val instants = Json.decodeFromString(
            MpInstantRange.serializer(),
            Json.encodeToString(MpInstantRange.serializer(), MpInstantRange.forever),
        )
        (instants.hasStart to instants.hasEnd) shouldBe (false to false)
    }

    "the open-end predicates at the neighbours of the sentinels" {
        val x = MpLocalDate.of(2024, 1, 1)
        val i = MpInstant.parse("2024-01-01T00:00:00Z")
        val oneMs = 1.milliseconds

        MpLocalDateRange(x, MpLocalDate.of(9999, 12, 31)).hasEnd shouldBe true
        MpLocalDateRange(x, MpLocalDate.of(10000, 1, 1)).hasEnd shouldBe false
        MpLocalDateRange(MpLocalDate.of(-10000, 1, 2), x).hasStart shouldBe true
        MpLocalDateRange(MpLocalDate.of(-10000, 1, 1), x).hasStart shouldBe false

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

        // through asOpenRange (to + 1 day)
        MpClosedLocalDateRange(x, MpLocalDate.of(9999, 12, 30)).hasEnd shouldBe true
        MpClosedLocalDateRange(x, MpLocalDate.of(9999, 12, 31)).hasEnd shouldBe false
        MpClosedLocalDateRange(MpLocalDate.of(-10000, 1, 2), x).hasStart shouldBe true
        MpClosedLocalDateRange(MpLocalDate.of(-10000, 1, 1), x).hasStart shouldBe false
    }

    "forever contains the last day of year 9999; the end is open from +10000-01-01T00:00Z on" {
        val i = MpInstant.parse("2024-01-01T00:00:00Z")

        MpInstantRange(i, MpInstant.parse("9999-12-31T23:59:59.999Z")).hasEnd shouldBe true
        MpLocalDateRange.forever.contains(MpLocalDate.of(9999, 12, 31)) shouldBe true
        MpInstantRange.forever.contains(MpInstant.parse("9999-12-31T12:00:00Z")) shouldBe true
    }

    "MpClosedLocalDateRange.beginningAt(d).asOpenRange stores to = 10000-01-02, a day past Doomsday" {
        val open = MpClosedLocalDateRange.beginningAt(MpLocalDate.of(2024, 1, 1)).asOpenRange

        open.to shouldBe MpLocalDate.of(10000, 1, 2)
        open.hasEnd shouldBe false
        Json.encodeToString(MpLocalDateRange.serializer(), open) shouldContain "\"ts\":253402387200000"
    }

    "dates outside Genesis to Doomsday stay valid plain values; a range from before Genesis has no start" {
        MpLocalDate.of(-12000, 3, 15).year shouldBe -12000
        MpLocalDate.of(12000, 1, 1).plusDays(1) shouldBe MpLocalDate.of(12000, 1, 2)

        MpLocalDateRange(MpLocalDate.of(-12000, 1, 1), MpLocalDate.of(2024, 1, 1)).hasStart shouldBe false
    }

    "numberOfDays counts a range that reaches past Doomsday only up to Doomsday (the documented clamp)" {
        val range = MpLocalDateRange(MpLocalDate.of(2024, 1, 1), MpLocalDate.of(12000, 1, 1))

        range.hasEnd shouldBe false
        MpLocalDateRange.forever.numberOfDays shouldBe 7_304_850

        // pinned as found: the clamp cuts the last noon to Doomsday's 00:00, and rounding half to even then counts
        // Doomsday itself for every second start date (2024-01-01 to 10000-01-01 is 2 913 174 days; reported in
        // ULTRA-00343)
        range.numberOfDays shouldBe 2_913_175
        MpLocalDateRange(MpLocalDate.of(2024, 1, 2), MpLocalDate.of(12000, 1, 1)).numberOfDays shouldBe 2_913_173
    }

    "Genesis seen in a zone west of UTC falls on -10001-12-31" {
        MpInstant.Genesis.atZone(MpTimezone.of("America/New_York")).toLocalDate() shouldBe
                MpLocalDate.of(-10001, 12, 31)
    }

    "DateTimeRangeConverter clamps a range past the sentinels to them" {
        val utc = MpTimezone.UTC
        val range = DateTimeRangeConverter(utc, MpLocalDate.of(-12000, 1, 1), MpLocalDate.of(12000, 1, 1))
            .fromHourToHour(fromHour = 12, toHour = 12)

        range.from shouldBe MpZonedDateTime.Genesis
        range.to shouldBe MpZonedDateTime.Doomsday
    }
})
