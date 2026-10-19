package io.peekandpoke.ultra.datetime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Suppress("unused")
class ConversionKotlinxSpec : StringSpec({

    "MpInstant <-> Instant keeps nanoseconds" {
        val instant = Instant.fromEpochSeconds(1_649_160_794, 123_456_789)

        instant.mp.toEpochSeconds() shouldBe 1_649_160_794
        instant.mp.kotlinx shouldBe instant
    }

    "MpLocalDate <-> LocalDate" {
        LocalDate(2022, 4, 5).mp shouldBe MpLocalDate.of(2022, 4, 5)
        MpLocalDate.of(2022, 4, 5).kotlinx shouldBe LocalDate(2022, 4, 5)
    }

    "MpLocalDateTime <-> LocalDateTime" {
        LocalDateTime(2022, 4, 5, 12, 13, 14, 15_000_000).mp shouldBe MpLocalDateTime.of(2022, 4, 5, 12, 13, 14, 15)
        MpLocalDateTime.of(2022, 4, 5, 12, 13, 14, 15).kotlinx shouldBe LocalDateTime(2022, 4, 5, 12, 13, 14, 15_000_000)
    }

    "MpLocalTime <-> LocalTime, truncated to milliseconds" {
        MpLocalTime.of(13, 14, 15, 678).kotlinx shouldBe LocalTime(13, 14, 15, 678_000_000)
        LocalTime(1, 2, 3, 456_789_000).mp shouldBe MpLocalTime.of(1, 2, 3, 456)
    }

    "MpLocalTime wraps below midnight, so its converter does not throw" {
        val time = MpLocalTime.of(0, 30).minus(1.hours)

        time shouldBe MpLocalTime.of(23, 30)
        time.kotlinx shouldBe LocalTime(23, 30)
    }

    "MpDatePeriod <-> DatePeriod, kotlinx normalises months into years" {
        MpDatePeriod(years = 1, months = 14, days = 3).kotlinx shouldBe DatePeriod(years = 2, months = 2, days = 3)
        DatePeriod(years = 1, months = 2, days = 3).mp shouldBe MpDatePeriod(years = 1, months = 2, days = 3)
    }

    "MpDateTimePeriod <-> DateTimePeriod" {
        val mp = MpDateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5, seconds = 6, milliseconds = 7)
        val kotlinx = DateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5, seconds = 6, nanoseconds = 7_000_000)

        mp.kotlinx shouldBe kotlinx
        kotlinx.mp shouldBe mp
    }

    "the system zone, however kotlinx names it, is MpTimezone.systemDefault" {
        MpTimezone.of("SYSTEM") shouldBe MpTimezone.systemDefault
        TimeZone.currentSystemDefault().mp shouldBe MpTimezone.systemDefault
    }

    "supported ids do not collide case-insensitively, so case variants can be told apart" {
        MpTimezone.supportedIds.groupBy { it.lowercase() }.filterValues { it.size > 1 } shouldBe emptyMap()
    }
})
