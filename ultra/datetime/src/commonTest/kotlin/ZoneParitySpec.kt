package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/** Zone math must match java.time on every target — the browser (Intl), the JVM and native. */
@Suppress("unused")
class ZoneParitySpec : StringSpec({

    ZoneParityTable.localCases.forEach { case ->
        "local ${case.local} in ${case.zone} resolves like java.time" {
            val timezone = MpTimezone.of(case.zone)
            val local = LocalDateTime.parse(case.local).mp

            val instant = local.toInstant(timezone)

            instant.toEpochSeconds() shouldBe case.epochSeconds
            timezone.offsetSecondsAt(instant) shouldBe case.offsetSeconds
            instant.atZone(timezone).toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal).mp
            local.atZone(timezone).toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal).mp
        }
    }

    ZoneParityTable.startOfDayCases.forEach { case ->
        "start of ${case.date} in ${case.zone} is the one java.time picks" {
            val timezone = MpTimezone.of(case.zone)

            val start = LocalDate.parse(case.date).mp.atStartOfDay(timezone)

            start.toEpochSeconds() shouldBe case.epochSeconds
            start.toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal).mp
        }
    }

    ZoneParityTable.plusCases.forEach { case ->
        "plus ${case.months} months and ${case.days} days from ${case.startEpochSeconds} in ${case.zone}" {
            val timezone = MpTimezone.of(case.zone)

            val result = MpInstant.fromEpochSeconds(case.startEpochSeconds)
                .plus(case.months, DateTimeUnit.MONTH, timezone)
                .plus(case.days, DateTimeUnit.DAY, timezone)

            result.toEpochSeconds() shouldBe case.epochSeconds
            result.atZone(timezone).toLocalDateTime() shouldBe LocalDateTime.parse(case.resultLocal).mp
        }
    }
})
