package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.minutes

@Suppress("unused")
class MpInstantRangeSpec : StringSpec({

    val now = MpInstant.now()

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

    "contains(other: PortableDate)" {

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
