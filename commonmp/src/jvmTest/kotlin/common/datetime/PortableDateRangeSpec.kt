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

            "intersects(other: PortableDateRange)" - {

                "Two valid ranges with a gap in between, do not intersect" {

                    val one = PortableDateRange(
                        from = now.plusDays(1).portable,
                        to = now.plusDays(2).portable
                    )
                    val two = PortableDateRange(
                        from = now.plusDays(3).portable,
                        to = now.plusDays(4).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe false
                    two.intersects(one) shouldBe false
                }

                "When one range is invalid, they do not intersect" {

                    val valid = PortableDateRange(
                        from = now.plusDays(1).portable,
                        to = now.plusDays(2).portable
                    )

                    val invalid = PortableDateRange(
                        from = now.plusDays(4).portable,
                        to = now.plusDays(3).portable
                    )

                    valid.isValid shouldBe true
                    invalid.isValid shouldBe false

                    valid.intersects(invalid) shouldBe false
                    invalid.intersects(valid) shouldBe false
                }

                "Two valid ranges that are touching back to back do not intersect" {

                    val touch = now.plusDays(1)

                    val one = PortableDateRange(
                        from = now.portable, to = touch.portable
                    )
                    val two = PortableDateRange(
                        from = touch.portable, to = now.plusDays(2).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe false
                    two.intersects(one) shouldBe false
                }

                "Two identical valid ranges do intersect" {

                    val range = PortableDateRange(
                        from = now.portable, to = now.plusDays(1).portable
                    )

                    range.isValid shouldBe true

                    range.intersects(range) shouldBe true
                }

                "Two identical invalid ranges do not intersect" {

                    val range = PortableDateRange(
                        from = now.portable, to = now.minusDays(1).portable
                    )

                    range.isValid shouldBe false

                    range.intersects(range) shouldBe false
                }

                "A valid range containing another valid range also intersects with it" {

                    val inner = PortableDateRange(
                        from = now.plusDays(1).portable, to = now.plusDays(2).portable
                    )
                    val outer = PortableDateRange(
                        from = now.plusDays(0).portable, to = now.plusDays(3).portable
                    )

                    inner.isValid shouldBe true
                    outer.isValid shouldBe true

                    inner.intersects(outer) shouldBe true
                    outer.intersects(inner) shouldBe true
                }

                "Two valid ranges with the same 'to' intersect" {

                    val end = now.plusDays(2)

                    val one = PortableDateRange(
                        from = now.plusDays(0).portable, to = end.portable
                    )
                    val two = PortableDateRange(
                        from = now.plusDays(1).portable, to = end.portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe true
                    two.intersects(one) shouldBe true
                }

                "Two valid ranges with the same 'from' intersect" {

                    val from = now

                    val one = PortableDateRange(
                        from = from.portable, to = now.plusDays(1).portable
                    )
                    val two = PortableDateRange(
                        from = from.portable, to = now.plusDays(2).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe true
                    two.intersects(one) shouldBe true
                }
            }
        }
    }
}
