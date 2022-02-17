package de.peekandpoke.ultra.foundation.spacetime

import de.peekandpoke.ultra.foundation.timing.Kronos
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class KronosSpec : FreeSpec() {

    private val fixedDate = LocalDate.of(2022, Month.FEBRUARY, 16)
    private val fixedTime = LocalTime.of(12, 0)
    private val fixedZone = ZoneOffset.UTC

    private val fixedInstant = LocalDateTime.of(fixedDate, fixedTime).toInstant(fixedZone)
    private val fixedClock = Clock.fixed(fixedInstant, fixedZone)

    init {

        "Kronos.fromClock automatic checks" - {

            val subject: Kronos = Kronos.fromClock(fixedClock)

            "timezone" {
                subject.timezone() shouldBe fixedZone
            }

            "instantNow" {
                subject.instantNow() shouldBe fixedInstant
            }

            "localDateNow" {
                subject.localDateNow() shouldBe fixedDate
            }

            "localDateTimeNow" {
                subject.localDateTimeNow() shouldBe LocalDateTime.of(fixedDate, fixedTime)
            }

            "zonedDateTimeNow" {
                subject.zonedDateTimeNow() shouldBe ZonedDateTime.of(fixedDate, fixedTime, fixedZone)
            }

            "zonedDateTimeNow(timezone)" {

                val zones = ZoneId.getAvailableZoneIds()
                    .map { ZoneId.of(it) }
                    .plus(ZoneOffset.UTC)

                val zipped = zones.zip(zones)

                repeat(1000) {
                    val (first, second) = zipped.random()

                    withClue("epoch timestamp must the same for '$first' and '$second'") {
                        subject.zonedDateTimeNow(first).toEpochSecond() shouldBe
                                subject.zonedDateTimeNow(second).toEpochSecond()
                    }
                }

                zones.forEach { zone ->

                    withClue("Must work for zone ${zone.id}") {
                        subject.zonedDateTimeNow(zone) shouldBe
                                ZonedDateTime.of(fixedDate, fixedTime, fixedZone).withZoneSameInstant(zone)
                    }
                }
            }

            "localTimeNow" {
                subject.localTimeNow() shouldBe fixedTime
            }
        }

        "Kronos.advanceBy automatic checks" - {

            val advances = listOf(
                Duration.ZERO
                    .plusDays(-1).plusHours(-1),
                Duration.ZERO
                    .plusHours(-2).plusMinutes(-2).plusSeconds(-2),
                Duration.ZERO,
                Duration.ZERO
                    .plusHours(2).plusMinutes(2).plusSeconds(2),
                Duration.ZERO
                    .plusDays(1).plusHours(1),
            )

            advances.forEach { advance ->
                "by duration $advance" - {

                    val subject: Kronos = Kronos.fromClock(fixedClock).advanceBy { advance }

                    "timezone" {
                        subject.timezone() shouldBe fixedZone
                    }

                    "instantNow" {
                        subject.instantNow() shouldBe fixedInstant.plus(advance)
                    }

                    "localDateNow" {
                        subject.localDateNow() shouldBe fixedDate.plusDays(advance.toDays())
                    }

                    "localDateTimeNow" {
                        subject.localDateTimeNow() shouldBe
                                LocalDateTime.of(fixedDate, fixedTime).plus(advance)
                    }

                    "zonedDateTimeNow" {
                        subject.zonedDateTimeNow() shouldBe
                                ZonedDateTime.of(fixedDate, fixedTime, fixedZone).plus(advance)
                    }

                    "zonedDateTimeNow(timezone)" {
                        val zone = ZoneId.of("Asia/Singapore")

                        subject.zonedDateTimeNow(zone) shouldBe
                                ZonedDateTime.of(fixedDate, fixedTime, fixedZone)
                                    .withZoneSameInstant(zone)
                                    .plus(advance)
                    }

                    "localTimeNow" {
                        subject.localTimeNow() shouldBe fixedTime.plus(advance)
                    }
                }
            }
        }
    }
}