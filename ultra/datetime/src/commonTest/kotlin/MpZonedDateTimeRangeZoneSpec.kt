package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.hours

/**
 * ULTRA-00211, delta round 3: an [MpZonedDateTimeRange] compares its endpoints by instant, so no predicate depends on
 * the zone of an endpoint (the `compareTo` tie-break orders one instant in two zones by local time and zone id).
 */
@Suppress("unused")
class MpZonedDateTimeRangeZoneSpec : StringSpec({

    val utc = MpTimezone.UTC
    val berlin = MpTimezone.of("Europe/Berlin")
    val newYork = MpTimezone.of("America/New_York")
    val london = MpTimezone.of("Europe/London")

    fun at(iso: String, zone: MpTimezone) = MpInstant.parse(iso).atZone(zone)

    listOf(newYork, berlin, london).forEach { zone ->
        "beginningAt(x).atZone(${zone.id}): no end, open" {
            val range = MpZonedDateTimeRange.beginningAt(at("2022-07-01T10:00:00Z", berlin)).atZone(zone)

            range.hasEnd shouldBe false
            range.isOpen shouldBe true
        }

        "forever.atZone(${zone.id}): no start, no end" {
            val range = MpZonedDateTimeRange.forever.atZone(zone)

            range.hasStart shouldBe false
            range.hasEnd shouldBe false
        }
    }

    listOf(berlin to utc, utc to berlin).forEach { (zoneA, zoneB) ->
        "[10:00Z, 12:00Z] in ${zoneA.id} and [12:00Z, 14:00Z] in ${zoneB.id}: touch, do not intersect" {
            val a = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", zoneA), at("2022-07-01T12:00:00Z", zoneA))
            val b = MpZonedDateTimeRange(at("2022-07-01T12:00:00Z", zoneB), at("2022-07-01T14:00:00Z", zoneB))

            a.intersects(b) shouldBe false
            b.intersects(a) shouldBe false
            a.touches(b) shouldBe true
            b.touches(a) shouldBe true
        }
    }

    "[10:00Z, 12:00Z] in Berlin contains [10:00Z, 11:00Z] in UTC" {
        val outer = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", berlin), at("2022-07-01T12:00:00Z", berlin))
        val inner = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", utc), at("2022-07-01T11:00:00Z", utc))

        outer.contains(inner) shouldBe true
    }

    "from and to one instant in two zones: never valid, in both orders" {
        val inUtc = at("2022-07-01T10:00:00Z", utc)
        val inBerlin = at("2022-07-01T10:00:00Z", berlin)

        MpZonedDateTimeRange(inUtc, inBerlin).isValid shouldBe false
        MpZonedDateTimeRange(inBerlin, inUtc).isValid shouldBe false
        MpZonedDateTimeRange.Partial(inUtc, inBerlin).asValidRange() shouldBe null
        MpZonedDateTimeRange.Partial(inBerlin, inUtc).asValidRange() shouldBe null
    }

    "compareTo(period): one hour from UTC to Berlin equals one hour" {
        val range = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", utc), at("2022-07-01T11:00:00Z", berlin))

        range.compareTo(MpDateTimePeriod.of(1.hours)) shouldBe 0
    }

    "mergeWith keeps this range's endpoints on a tie" {
        val a = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", utc), at("2022-07-01T12:00:00Z", utc))
        val b = MpZonedDateTimeRange(at("2022-07-01T10:00:00Z", berlin), at("2022-07-01T12:00:00Z", berlin))

        a.mergeWith(b) shouldBe listOf(a)
        b.mergeWith(a) shouldBe listOf(b)
    }

    "a range stored after atZone(New York) decodes without an end (kotlinx Json)" {
        val range = MpZonedDateTimeRange.beginningAt(at("2022-07-01T10:00:00Z", berlin)).atZone(newYork)

        val decoded = Json.decodeFromString(
            MpZonedDateTimeRange.serializer(),
            Json.encodeToString(MpZonedDateTimeRange.serializer(), range),
        )

        decoded.hasEnd shouldBe false
        decoded shouldBe range
    }

    "DateTimeRangeConverter clamps to Genesis / Doomsday by instant; on a tie Genesis / Doomsday stay, as before" {
        val plusOne = MpTimezone.of("+01:00")
        // -10000-01-01T01:00+01:00 is Genesis' instant, +10000-01-01T01:00+01:00 is Doomsday's: both ties
        val converter = DateTimeRangeConverter(plusOne, MpLocalDate.of(-10000, 1, 1), MpLocalDate.of(10000, 1, 1))

        val range = converter.fromHourToHour(fromHour = 1, toHour = 1)

        range.from shouldBe MpZonedDateTime.Genesis
        range.to shouldBe MpZonedDateTime.Doomsday

        val past = DateTimeRangeConverter(plusOne, MpLocalDate.of(-10000, 1, 1), MpLocalDate.of(-10000, 1, 1))
            .fromHourToHour(fromHour = 0, toHour = 1)

        past.from shouldBe MpZonedDateTime.Genesis
    }
})
