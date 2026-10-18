package io.peekandpoke.ultra.datetime

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * java.time is the spec for ultra's zone math.
 *
 * The table rows are re-derived from java.time, so [ZoneParityTable] cannot drift. The sweep then checks the shared
 * algorithm in `TimezoneEngine` against java.time around every transition of every zone the JDK knows.
 */
@Suppress("unused")
class ZoneParityJvmSpec : StringSpec({

    "every LocalCase row is what java.time computes" {
        ZoneParityTable.localCases.forEach { case ->
            withClue(case) {
                val zoned = ZonedDateTime.ofLocal(LocalDateTime.parse(case.local), ZoneId.of(case.zone), null)

                zoned.toEpochSecond() shouldBe case.epochSeconds
                zoned.offset.totalSeconds shouldBe case.offsetSeconds
                zoned.toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal)
            }
        }
    }

    "every StartOfDayCase row is what java.time computes" {
        ZoneParityTable.startOfDayCases.forEach { case ->
            withClue(case) {
                val start = LocalDate.parse(case.date).atStartOfDay(ZoneId.of(case.zone))

                start.toEpochSecond() shouldBe case.epochSeconds
                start.toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal)
            }
        }
    }

    "every PlusCase row is what java.time computes" {
        ZoneParityTable.plusCases.forEach { case ->
            withClue(case) {
                val result = Instant.ofEpochSecond(case.startEpochSeconds).atZone(ZoneId.of(case.zone))
                    .plusMonths(case.months.toLong())
                    .plusDays(case.days.toLong())

                result.toEpochSecond() shouldBe case.epochSeconds
                result.toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal)
            }
        }
    }

    "the engine matches java.time around every transition of every zone, 1900 to 2040" {
        val from = Instant.parse("1900-01-01T00:00:00Z")
        val until = Instant.parse("2040-01-01T00:00:00Z")

        val mismatches = mutableListOf<String>()
        var checks = 0

        fun check(what: String, expected: Long, actual: Long) {
            checks++
            if (expected != actual) mismatches += "$what: expected $expected, got $actual"
        }

        ZoneId.getAvailableZoneIds().sorted().forEach { zoneId ->
            val zone = ZoneId.of(zoneId)
            val timezone = MpTimezone.of(zoneId)
            var transition = zone.rules.nextTransition(from)

            while (transition != null && transition.instant < until) {
                val earliest = minOf(transition.dateTimeBefore, transition.dateTimeAfter)
                val latest = maxOf(transition.dateTimeBefore, transition.dateTimeAfter)
                val middle = earliest.plusSeconds((latest.toEpochSecond(ZoneOffset.UTC) - earliest.toEpochSecond(ZoneOffset.UTC)) / 2)

                val probes = listOf(
                    earliest.minusSeconds(1), earliest, middle, latest.minusSeconds(1), latest, latest.plusSeconds(1),
                )

                probes.forEach { local ->
                    check(
                        what = "$zoneId toInstant($local)",
                        expected = ZonedDateTime.ofLocal(local, zone, null).toEpochSecond(),
                        actual = local.mp.toInstant(timezone).toEpochSeconds(),
                    )

                    // Arriving at the probe by whole days, from both sides of the transition: in an overlap the
                    // offset we came from must be kept.
                    listOf(-1, 1).forEach { days ->
                        val start = ZonedDateTime.ofLocal(local.minusDays(days.toLong()), zone, null)

                        check(
                            what = "$zoneId ${start.toOffsetDateTime()} plus $days days",
                            expected = start.plusDays(days.toLong()).toEpochSecond(),
                            actual = start.toInstant().mp.plus(days, DateTimeUnit.DAY, timezone).toEpochSeconds(),
                        )
                    }
                }

                listOf(transition.instant.minusSeconds(1), transition.instant).forEach { instant ->
                    check(
                        what = "$zoneId offset at $instant",
                        expected = zone.rules.getOffset(instant).totalSeconds.toLong(),
                        actual = timezone.offsetSecondsAt(instant.mp).toLong(),
                    )
                }

                setOf(earliest.toLocalDate(), latest.toLocalDate()).forEach { date ->
                    check(
                        what = "$zoneId start of $date",
                        expected = date.atStartOfDay(zone).toEpochSecond(),
                        actual = date.mp.atStartOfDay(timezone).toEpochSeconds(),
                    )
                }

                transition = zone.rules.nextTransition(transition.instant)
            }
        }

        println("ZoneParityJvmSpec: $checks checks across ${ZoneId.getAvailableZoneIds().size} zones")

        checks shouldBeGreaterThan 100_000
        mismatches.take(50).shouldBeEmpty()
    }
})
