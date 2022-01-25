package de.peekandpoke.ultra.common.datetime

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PartialPortableDateRangeSpec : FreeSpec() {

    private val now = LocalDate.now()

    init {

        "Conversion" - {

            "asValidDateRange" - {

                "'from' == null will return null" {

                    val subject = PartialPortableDateRange(from = null, to = now.portable)

                    subject.asValidDateRange shouldBe null
                }

                "'to' == null will return null" {

                    val subject = PartialPortableDateRange(from = now.portable, to = null)

                    subject.asValidDateRange shouldBe null
                }

                "'from' before 'to' will return a PortableDateRange" {

                    val from = now.portable
                    val to = now.plusDays(1).portable

                    val subject = PartialPortableDateRange(from = from, to = to)

                    subject.asValidDateRange shouldBe PortableDateRange(from = from, to = to)
                }

                "'from' == 'to' will return null" {

                    val from = now.portable
                    val to = from

                    val subject = PartialPortableDateRange(from = from, to = to)

                    subject.asValidDateRange shouldBe null
                }
            }
        }
    }
}
