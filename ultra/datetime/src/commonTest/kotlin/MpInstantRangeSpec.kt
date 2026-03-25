package de.peekandpoke.ultra.datetime

import de.peekandpoke.ultra.common.tuple

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class MpInstantRangeSpec : StringSpec({

    val now = MpInstant.now()

    "Creation - forever" {
        val forever = MpInstantRange.forever

        forever.from shouldBe MpInstant.Genesis
        forever.to shouldBe MpInstant.Doomsday

        forever.duration shouldBe 7304850.days

        forever.isValid shouldBe true

        forever.hasStart shouldBe false
        forever.hasEnd shouldBe false

        forever.isOpen shouldBe true
        forever.isNotOpen shouldBe false
    }

    "Creation - of from and duration" {
        val result = MpInstantRange.of(
            MpInstant.parse("2022-04-01T00:00:00Z"),
            1.hours
        )

        result shouldBe MpInstantRange(
            from = MpInstant.parse("2022-04-01T00:00:00Z"),
            to = MpInstant.parse("2022-04-01T01:00:00Z"),
        )
    }

    "Creation - beginningAt" {
        val result = MpInstantRange.beginningAt(
            MpInstant.parse("2022-04-01T00:00:00Z")
        )

        result shouldBe MpInstantRange(
            MpInstant.parse("2022-04-01T00:00:00Z"),
            MpInstant.Doomsday,
        )

        result.isValid shouldBe true

        result.hasStart shouldBe true
        result.hasEnd shouldBe false

        result.isOpen shouldBe true
        result.isNotOpen shouldBe false
    }

    "Creation - endingAt" {
        val result = MpInstantRange.endingAt(
            MpInstant.parse("2022-04-01T00:00:00Z")
        )

        result shouldBe MpInstantRange(
            MpInstant.Genesis,
            MpInstant.parse("2022-04-01T00:00:00Z"),
        )

        result.isValid shouldBe true

        result.hasStart shouldBe false
        result.hasEnd shouldBe true

        result.isOpen shouldBe true
        result.isNotOpen shouldBe false
    }

    "duration" {
        MpInstantRange(
            from = MpInstant.parse("2022-04-01T00:00:00Z"),
            to = MpInstant.parse("2022-04-01T01:00:00Z"),
        ).duration shouldBe 1.hours

        MpInstantRange(
            from = MpInstant.parse("2022-04-01T00:00:00Z"),
            to = MpInstant.parse("2022-04-01T00:00:00Z"),
        ).duration shouldBe Duration.ZERO

        MpInstantRange(
            from = MpInstant.parse("2022-04-01T00:00:00.001Z"),
            to = MpInstant.parse("2022-04-01T00:00:00.000Z"),
        ).duration shouldBe (-1).milliseconds
    }

    "Validity - When 'from' is earlier then 'to' the range is valid" {

        val subject = MpInstantRange(from = now, to = now.plus(1.minutes))

        subject.isValid shouldBe true
    }

    "Validity - When 'from' is equal to 'to' the range is invalid" {

        val subject = MpInstantRange(from = now, to = now)

        subject.isValid shouldBe false
    }

    "Validity - When 'from' is later then 'to' the range is invalid" {

        val subject = MpInstantRange(from = now, to = now.minus(1.minutes))

        subject.isValid shouldBe false
    }

    "atZone" {

        now.toRange(10.minutes).let {

            val timezone = MpTimezone.of("Europe/Berlin")

            val zoned = it.atZone(timezone)

            zoned.from shouldBe it.from.atZone(timezone)
            zoned.to shouldBe it.to.atZone(timezone)
        }
    }

    "atSystemDefaultZone" {

        now.toRange(10.minutes).let {

            val zoned = it.atSystemDefaultZone()

            zoned.from shouldBe it.from.atSystemDefaultZone()
            zoned.to shouldBe it.to.atSystemDefaultZone()
        }
    }

    "contains(datetime)" {
        val validRange = now.toRange(1.days)

        withClue("The range does not contain anything before its own [from]") {
            validRange.contains(validRange.from.minus(1.milliseconds)) shouldBe false
        }

        withClue("The range contains its own [from]") {
            validRange.contains(validRange.from) shouldBe true
        }

        withClue("The range contains anything between [from] and [to]") {
            validRange.contains(now.plus(1.milliseconds)) shouldBe true
            validRange.contains(validRange.to.minus(1.milliseconds)) shouldBe true
        }

        withClue("The range does not contain its own [to]") {
            validRange.contains(validRange.to) shouldBe false
        }

        withClue("The range does not contain anything after its own [to]") {
            validRange.contains(validRange.to.plus(1.milliseconds)) shouldBe false
        }
    }

    "contains(other: MpAbsoluteDateTime)" {

        @Suppress("UnnecessaryVariable")
        val from = now
        val to = now.plus(2.minutes)

        val range = MpInstantRange(from = from, to = to)

        withClue("A date before the start date is not contained") {
            range.contains(from.minus(1.minutes)) shouldBe false
        }

        withClue("The from date is contained") {
            range.contains(now) shouldBe true
        }

        withClue("A date between from and to is contained") {
            range.contains(now.plus(1.minutes)) shouldBe true
        }

        withClue("The end date is not contained") {
            range.contains(to) shouldBe false
        }
    }

    "contains(other: MpInstantRange)" {

        withClue("A valid range contains itself") {

            val range = MpInstantRange(from = now, to = now.plus(1.minutes))

            range.contains(range) shouldBe true
        }

        withClue("A invalid range does not contain itself") {

            val range = MpInstantRange(from = now, to = now.minus(1.minutes))

            range.contains(range) shouldBe false
        }

        withClue("A valid range does not contain an invalid range and vice versa") {

            val valid = MpInstantRange(from = now, to = now.plus(1.minutes))
            val invalid = MpInstantRange(from = now.plus(1.minutes), to = now)

            valid.contains(invalid) shouldBe false
            invalid.contains(valid) shouldBe false
        }
    }

    listOf(
        tuple(
            "All eaten up - exactly",
            now.toRange(1.hours),
            now.toRange(1.hours),
            listOf(),
        ),
        tuple(
            "All eaten up - surrounding",
            now.toRange(1.hours),
            now.minus(1.seconds).toRange(1.hours + 2.seconds),
            listOf(),
        ),
        tuple(
            "In the middle",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(1.hours),
            listOf(
                now.toRange(1.hours),
                now.plus(2.hours).toRange(1.hours),
            ),
        ),
        tuple(
            "In the middle but duration = 0",
            now.toRange(3.hours),
            now.plus(1.hours).toRange(0.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        tuple(
            "On the left side",
            now.toRange(3.hours),
            now.toRange(1.hours),
            listOf(
                now.plus(1.hours).toRange(2.hours)
            ),
        ),
        tuple(
            "On the right side",
            now.toRange(3.hours),
            now.plus(2.hours).toRange(1.hours),
            listOf(
                now.toRange(2.hours)
            ),
        ),
        tuple(
            "Too far on the left side",
            now.toRange(3.hours),
            now.minus(2.hours).toRange(2.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
        tuple(
            "Too far on the right side",
            now.toRange(3.hours),
            now.plus(3.hours).toRange(2.hours),
            listOf(
                now.toRange(3.hours),
            ),
        ),
    ).forEach { (title, source, other, expected) ->

        ("cutAway(other: MpInstantRange) - " +
                "$title: ${source.format()} cut away ${other.format()} " +
                "must be ${expected.format()}  ") {

            source.cutAway(other) shouldBe expected
        }
    }

    // isNotValid ////////////////////////////////////////////////////////////////////////////////

    "isNotValid is the inverse of isValid" {
        val valid = now.toRange(1.hours)
        valid.isNotValid shouldBe false

        val invalid = MpInstantRange(from = now, to = now.minus(1.minutes))
        invalid.isNotValid shouldBe true

        val zero = MpInstantRange(from = now, to = now)
        zero.isNotValid shouldBe true
    }

    // Partial ////////////////////////////////////////////////////////////////////////////////

    "Partial.empty has null from and to" {
        MpInstantRange.Partial.empty.from shouldBe null
        MpInstantRange.Partial.empty.to shouldBe null
    }

    "Partial.asValidRange returns valid range" {
        val from = MpInstant.parse("2024-01-01T00:00:00Z")
        val to = MpInstant.parse("2024-01-02T00:00:00Z")

        MpInstantRange.Partial(from, to).asValidRange() shouldBe MpInstantRange(from, to)
    }

    "Partial.asValidRange returns null for invalid range" {
        val from = MpInstant.parse("2024-01-02T00:00:00Z")
        val to = MpInstant.parse("2024-01-01T00:00:00Z")

        MpInstantRange.Partial(from, to).asValidRange() shouldBe null
    }

    "Partial.asValidRange returns null when from or to is null" {
        val instant = MpInstant.parse("2024-01-01T00:00:00Z")

        MpInstantRange.Partial(instant, null).asValidRange() shouldBe null
        MpInstantRange.Partial(null, instant).asValidRange() shouldBe null
        MpInstantRange.Partial.empty.asValidRange() shouldBe null
    }

    // intersects ////////////////////////////////////////////////////////////////////////////////

    "intersects - overlapping ranges" {
        val a = now.toRange(2.hours)
        val b = MpInstantRange(from = now.plus(1.hours), to = now.plus(3.hours))

        a.intersects(b) shouldBe true
        b.intersects(a) shouldBe true
    }

    "intersects - non-overlapping ranges" {
        val a = now.toRange(1.hours)
        val b = MpInstantRange(from = now.plus(2.hours), to = now.plus(3.hours))

        a.intersects(b) shouldBe false
        b.intersects(a) shouldBe false
    }

    "intersects - adjacent ranges do not intersect" {
        val a = now.toRange(1.hours)
        val b = MpInstantRange(from = now.plus(1.hours), to = now.plus(2.hours))

        a.intersects(b) shouldBe false
    }

    "intersects - one contains the other" {
        val outer = now.toRange(4.hours)
        val inner = MpInstantRange(from = now.plus(1.hours), to = now.plus(2.hours))

        outer.intersects(inner) shouldBe true
        inner.intersects(outer) shouldBe true
    }

    "intersects - invalid range returns false" {
        val valid = now.toRange(1.hours)
        val invalid = MpInstantRange(from = now.plus(1.hours), to = now)

        valid.intersects(invalid) shouldBe false
    }

    // plus / minus ////////////////////////////////////////////////////////////////////////////////

    "plus(duration) shifts both from and to" {
        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-01T00:00:00Z"),
            to = MpInstant.parse("2024-01-01T01:00:00Z"),
        )

        val shifted = range.plus(2.hours)

        shifted.from shouldBe MpInstant.parse("2024-01-01T02:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-01T03:00:00Z")
    }

    "minus(duration) shifts both from and to backwards" {
        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-01T04:00:00Z"),
            to = MpInstant.parse("2024-01-01T05:00:00Z"),
        )

        val shifted = range.minus(2.hours)

        shifted.from shouldBe MpInstant.parse("2024-01-01T02:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-01T03:00:00Z")
    }

    "plus(unit, timezone) shifts by calendar unit" {
        val utc = TimeZone.UTC

        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-15T10:00:00Z"),
            to = MpInstant.parse("2024-01-15T11:00:00Z"),
        )

        val shifted = range.plus(DateTimeUnit.DAY, utc)

        shifted.from shouldBe MpInstant.parse("2024-01-16T10:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-16T11:00:00Z")
    }

    "plus(value, unit, timezone) shifts by multiple calendar units" {
        val utc = TimeZone.UTC

        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-15T10:00:00Z"),
            to = MpInstant.parse("2024-01-15T11:00:00Z"),
        )

        val shifted = range.plus(3, DateTimeUnit.DAY, utc)

        shifted.from shouldBe MpInstant.parse("2024-01-18T10:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-18T11:00:00Z")
    }

    "minus(unit, timezone) shifts backwards by calendar unit" {
        val utc = TimeZone.UTC

        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-15T10:00:00Z"),
            to = MpInstant.parse("2024-01-15T11:00:00Z"),
        )

        val shifted = range.minus(DateTimeUnit.DAY, utc)

        shifted.from shouldBe MpInstant.parse("2024-01-14T10:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-14T11:00:00Z")
    }

    "minus(value, unit, timezone) shifts backwards by multiple calendar units" {
        val utc = TimeZone.UTC

        val range = MpInstantRange(
            from = MpInstant.parse("2024-01-15T10:00:00Z"),
            to = MpInstant.parse("2024-01-15T11:00:00Z"),
        )

        val shifted = range.minus(3, DateTimeUnit.DAY, utc)

        shifted.from shouldBe MpInstant.parse("2024-01-12T10:00:00Z")
        shifted.to shouldBe MpInstant.parse("2024-01-12T11:00:00Z")
    }

    "plus preserves duration" {
        val range = now.toRange(1.hours)
        val shifted = range.plus(5.days)

        shifted.duration shouldBe range.duration
    }

    // Partial.asDateRange /////////////////////////////////////////////////////////////////////////

    "Partial.asDateRange converts to MpClosedLocalDateRange.Partial" {
        val from = MpInstant.parse("2024-06-15T10:00:00Z")
        val to = MpInstant.parse("2024-06-20T10:00:00Z")
        val timezone = MpTimezone.UTC

        val partial = MpInstantRange.Partial(from, to)
        val dateRange = partial.asDateRange(timezone)

        dateRange.from shouldBe from.toLocalDate(timezone)
        dateRange.to shouldBe to.toLocalDate(timezone)
    }

    "Partial.asDateRange with null values produces null dates" {
        val timezone = MpTimezone.UTC

        val partial = MpInstantRange.Partial.empty
        val dateRange = partial.asDateRange(timezone)

        dateRange.from shouldBe null
        dateRange.to shouldBe null
    }
})
