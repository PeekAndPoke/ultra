package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    "Creation - beginningAt" {
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

    "Creation - endingAt" {
        val result = MpInstantRange.beginningAt(
            MpInstant.parse("2022-04-01T00:00:00Z")
        )

        result shouldBe MpInstantRange(
            MpInstant.parse("2022-04-01T00:00:00Z"),
            MpInstant.Doomsday,
        )
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

    "contains(other: MpInstantRange)" {

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
})
