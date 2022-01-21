package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PortableDateRangeSpec : FreeSpec() {

    private val now = LocalDate.now()

    init {
        "Validity" - {

            "When 'from' is earlier then 'to' the range is valid" {

                val subject = PortableDateRange(from = now.portable, to = now.plusDays(1).portable)

                subject.isValid shouldBe true
            }

            "When 'from' is equal to 'to' the range is invalid" {

                val subject = PortableDateRange(from = now.portable, to = now.portable)

                subject.isValid shouldBe false
            }

            "When 'from' is later then 'to' the range is invalid" {

                val subject = PortableDateRange(from = now.portable, to = now.minusDays(1).portable)

                subject.isValid shouldBe false
            }
        }

        "Intersection" - {

            "contains(other: PortableDate)" - {

                val from = now
                val to = now.plusDays(2)

                val range = PortableDateRange(from = from.portable, to = to.portable)

                "A date before the start date is not contained" {
                    range.contains(from.minusDays(1).portable) shouldBe false
                }

                "The from date is contained" - {
                    range.contains(now.portable) shouldBe true
                }

                "A date between from and to is contained" - {
                    range.contains(now.plusDays(1).portable) shouldBe true
                }

                "The end date is not contained" - {
                    range.contains(now.plusDays(2).portable) shouldBe false
                }
            }

            "contains(other: PortableDateRange)" - {
                "A valid range contains itself" {

                    val range = PortableDateRange(from = now.portable, to = now.plusDays(1).portable)

                    range.contains(range) shouldBe true
                }

                "A invalid range does not contain itself" {

                    val range = PortableDateRange(from = now.portable, to = now.minusDays(1).portable)

                    range.contains(range) shouldBe false
                }

                "A valid range does not contain an invalid range and vice versa" {

                    val valid = PortableDateRange(from = now.portable, to = now.plusDays(10).portable)
                    val invalid = PortableDateRange(from = now.plusDays(10).portable, to = now.portable)

                    valid.contains(invalid) shouldBe false
                    invalid.contains(valid) shouldBe false
                }
            }
        }
    }
}
