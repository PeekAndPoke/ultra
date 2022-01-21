package de.peekandpoke.ultra.common.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PortableDateTimeRangeSpec : FreeSpec() {

    private val now = LocalDateTime.now()

    init {
        "Validity" - {

            "When 'from' is earlier then 'to' the range is valid" {

                val subject = PortableDateTimeRange(from = now.portable, to = now.plusSeconds(1).portable)

                subject.isValid shouldBe true
            }

            "When 'from' is equal to 'to' the range is invalid" {

                val subject = PortableDateTimeRange(from = now.portable, to = now.portable)

                subject.isValid shouldBe false
            }

            "When 'from' is later then 'to' the range is invalid" {

                val subject = PortableDateTimeRange(from = now.portable, to = now.minusSeconds(1).portable)

                subject.isValid shouldBe false
            }
        }

        "Intersection" - {

            "contains(other: LocalDateTime)" - {

                val from = now
                val to = now.plusDays(2)

                val range = PortableDateTimeRange(from = from.portable, to = to.portable)

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

            "contains(other: DateTimeRange)" - {

                "A valid range contains itself" {
                    val subject = PortableDateTimeRange(
                        from = now.portable, to = now.plusSeconds(1).portable
                    )

                    subject.isValid shouldBe true

                    subject.contains(subject) shouldBe true
                }

                "An invalid range does not contain itself" {
                    val subject = PortableDateTimeRange(
                        from = now.portable, to = now.minusSeconds(1).portable
                    )

                    subject.isValid shouldBe false

                    subject.contains(subject) shouldBe false
                }

                "A valid range does not contain an invalid range an vice versa" {

                    val valid = PortableDateTimeRange(
                        from = now.portable,
                        to = now.plusSeconds(10).portable
                    )

                    val invalid = PortableDateTimeRange(
                        from = now.plusSeconds(5).portable,
                        to = now.minusSeconds(2).portable
                    )

                    valid.isValid shouldBe true
                    invalid.isValid shouldBe false

                    valid.contains(invalid) shouldBe false
                    invalid.contains(valid) shouldBe false
                }

                "Valid range can contain other valid ranges" {
                    val subject = PortableDateTimeRange(
                        from = now.portable,
                        to = now.plusMinutes(10).portable
                    )

                    subject.isValid shouldBe true

                    // Should contain
                    listOf(
                        PortableDateTimeRange(
                            from = now.plusMinutes(0).portable,
                            to = now.plusMinutes(1).portable
                        ),
                        PortableDateTimeRange(
                            from = now.plusMinutes(1).portable,
                            to = now.plusMinutes(10).portable
                        ),
                        PortableDateTimeRange(
                            from = now.plusMinutes(1).portable,
                            to = now.plusMinutes(9).portable
                        ),
                    ).forEach {
                        withClue("$subject should contain $it") {
                            it.isValid shouldBe true
                            subject.contains(it) shouldBe true
                        }
                    }

                    // Should not contain
                    listOf(
                        PortableDateTimeRange(
                            from = now.plusMinutes(-1).portable,
                            to = now.plusMinutes(0).portable
                        ),
                        PortableDateTimeRange(
                            from = now.plusMinutes(-1).portable,
                            to = now.plusMinutes(1).portable
                        ),
                        PortableDateTimeRange(
                            from = now.plusMinutes(9).portable,
                            to = now.plusMinutes(11).portable
                        ),
                        PortableDateTimeRange(
                            from = now.plusMinutes(10).portable,
                            to = now.plusMinutes(11).portable
                        ),
                    ).forEach {
                        withClue("$subject should not contain $it") {
                            it.isValid shouldBe true
                            subject.contains(it) shouldBe false
                        }
                    }
                }
            }

            "intersects(other: DateTimeRange)" - {

                "Two valid ranges with a gap in between, do not intersect" {

                    val one = PortableDateTimeRange(
                        from = now.plusDays(1).portable,
                        to = now.plusDays(2).portable
                    )
                    val two = PortableDateTimeRange(
                        from = now.plusDays(3).portable,
                        to = now.plusDays(4).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe false
                    two.intersects(one) shouldBe false
                }

                "When one range is invalid, they do not intersect" {

                    val valid = PortableDateTimeRange(
                        from = now.plusDays(1).portable,
                        to = now.plusDays(2).portable
                    )

                    val invalid = PortableDateTimeRange(
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

                    val one = PortableDateTimeRange(
                        from = now.portable, to = touch.portable
                    )
                    val two = PortableDateTimeRange(
                        from = touch.portable, to = now.plusDays(2).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe false
                    two.intersects(one) shouldBe false
                }

                "Two identical valid ranges do intersect" {

                    val range = PortableDateTimeRange(
                        from = now.portable, to = now.plusDays(1).portable
                    )

                    range.isValid shouldBe true

                    range.intersects(range) shouldBe true
                }

                "Two identical invalid ranges do not intersect" {

                    val range = PortableDateTimeRange(
                        from = now.portable, to = now.minusDays(1).portable
                    )

                    range.isValid shouldBe false

                    range.intersects(range) shouldBe false
                }

                "Two ranges that overlap by one second do intersect" {

                    val touch = now.plusDays(1)

                    val one = PortableDateTimeRange(
                        from = now.portable, to = touch.portable
                    )
                    val two = PortableDateTimeRange(
                        from = touch.minusSeconds(1).portable, to = now.plusDays(2).portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe true
                    two.intersects(one) shouldBe true
                }

                "A valid range containing another valid range also intersects with it" {

                    val inner = PortableDateTimeRange(
                        from = now.plusDays(1).portable, to = now.plusDays(2).portable
                    )
                    val outer = PortableDateTimeRange(
                        from = now.plusDays(0).portable, to = now.plusDays(3).portable
                    )

                    inner.isValid shouldBe true
                    outer.isValid shouldBe true

                    inner.intersects(outer) shouldBe true
                    outer.intersects(inner) shouldBe true
                }

                "Two valid ranges with the same 'to' intersect" {

                    val end = now.plusDays(2)

                    val one = PortableDateTimeRange(
                        from = now.plusDays(0).portable, to = end.portable
                    )
                    val two = PortableDateTimeRange(
                        from = now.plusDays(1).portable, to = end.portable
                    )

                    one.isValid shouldBe true
                    two.isValid shouldBe true

                    one.intersects(two) shouldBe true
                    two.intersects(one) shouldBe true
                }

                "Two valid ranges with the same 'from' intersect" {

                    val from = now

                    val one = PortableDateTimeRange(
                        from = from.portable, to = now.plusDays(1).portable
                    )
                    val two = PortableDateTimeRange(
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
