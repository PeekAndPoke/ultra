@file:JvmName("ConversionJvm")

package io.peekandpoke.ultra.datetime

import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Converts an [MpInstant] into a [java.time.Instant]
 */
val MpInstant.jvm: Instant
    get() {
        return Instant.ofEpochMilli(toEpochMillis())
    }

/**
 * Converts a [java.time.Instant] into an [MpInstant]
 */
val Instant.mp: MpInstant
    get() {
        return MpInstant.fromEpochMillis(
            millis = toEpochMilli()
        )
    }

/**
 * Converts an [MpLocalDateTime] into a [java.time.LocalDateTime]
 */
val MpLocalDateTime.jvm: LocalDateTime
    get() {
        return LocalDateTime.of(
            year,
            monthNumber,
            dayOfMonth,
            hour,
            minute,
            second,
            milliSecond * 1_000_000,
        )
    }

/**
 * Converts a [java.time.LocalDateTime] into an [MpLocalDateTime]
 */
val LocalDateTime.mp: MpLocalDateTime
    get() {
        return MpLocalDateTime.of(
            year = year,
            month = monthValue,
            day = dayOfMonth,
            hour = hour,
            minute = minute,
            second = second,
            milliSecond = nano / 1_000_000,
        )
    }

/**
 * Converts a [java.time.OffsetDateTime] into an [MpInstant]
 */
val OffsetDateTime.mp get(): MpInstant = toInstant().mp

/**
 * Converts a [java.time.OffsetDateTime] into an [MpZonedDateTime]
 */
fun OffsetDateTime.mp(zone: MpTimezone): MpZonedDateTime = mp.atZone(zone)

/**
 * Converts an [MpLocalDate] into a [java.time.LocalDate]
 */
val MpLocalDate.jvm: LocalDate
    get() {
        return LocalDate.of(year, monthNumber, day)
    }

/**
 * Converts a [java.time.LocalDate] into an [MpLocalDate]
 */
val LocalDate.mp: MpLocalDate
    get() {
        return MpLocalDate.of(year = year, month = monthValue, day = dayOfMonth)
    }

/**
 * Converts an [MpZonedDateTime] into a [java.time.ZonedDateTime]
 */
val MpZonedDateTime.jvm: ZonedDateTime
    get() {
        // By epoch second and nanos, not millis: exact over the whole range. The nanos are cut to whole milliseconds,
        // MpZonedDateTime's precision on the wire (maintainer, 2026-11-14).
        val instant = toInstant().kotlinx

        return ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond / 1_000_000 * 1_000_000L),
            timezone.jvm,
        )
    }

/**
 * Converts a [java.time.ZonedDateTime] into an [MpZonedDateTime]
 */
val ZonedDateTime.mp: MpZonedDateTime
    get() {
        // By epoch second and nanos, not millis: exact over the whole range. The nanos (0..999_999_999 after a floored
        // second) are cut to whole milliseconds, a floor like toEpochMilli() (maintainer, 2026-11-14).
        val instant = toInstant()

        return MpInstant.fromEpochSeconds(instant.epochSecond, instant.nano / 1_000_000 * 1_000_000).atZone(zone.mp)
    }

/**
 * Converts a [java.time.LocalTime] into an [MpLocalTime]
 */
val LocalTime.mp: MpLocalTime
    get() =
        MpLocalTime.ofMilliSeconds(milliSecondsOfDay = 1000 * (hour * 60 * 60 + minute * 60 + second))

/**
 * Converts an [MpLocalTime] into a [java.time.LocalTime]. [MpLocalTime.EndOfDay], which it cannot hold, gives
 * [LocalTime.MAX].
 */
val MpLocalTime.jvm: LocalTime
    get() = when (this) {
        // java.time cannot hold 24:00: the last instant of the day keeps the order
        MpLocalTime.EndOfDay -> LocalTime.MAX
        else -> LocalTime.ofSecondOfDay((inWholeMilliSeconds() / 1000).toLong())
    }

/**
 * Converts a [java.time.ZoneId] to a [TimeZone].
 */
val ZoneId.kotlinx: TimeZone get() = TimeZone.of(id)

/**
 * Converts a [java.time.ZoneId] to a [TimeZone].
 */
fun TimeZone.Companion.of(zone: ZoneId): TimeZone = of(zone.id)

/**
 * Converts a [TimeZone] into a [java.time.ZoneId].
 */
val TimeZone.jvm: ZoneId get() = ZoneId.of(id)

/**
 * Converts [java.time.ZoneId] into an [MpTimezone].
 */
val ZoneId.mp: MpTimezone get() = MpTimezone.of(id)

/**
 * Converts an [MpTimezone] into a [java.time.ZoneId]. An id not shaped like a zone id throws [IllegalTimeZoneException]
 * before java.time sees it.
 */
val MpTimezone.jvm: ZoneId
    get() {
        TimezoneEngine.requireConvertibleZoneId(id)

        return ZoneId.of(id)
    }

/**
 * Converts an [MpMonth] into a [java.time.Month].
 */
val MpMonth.jvm: Month get() = Month.of(number)

/**
 * Converts a [java.time.Month] into an [MpMonth].
 */
val Month.mp: MpMonth get() = MpMonth.of(value)

/**
 * Converts an [MpDayOfWeek] into a [java.time.DayOfWeek].
 */
val MpDayOfWeek.jvm: DayOfWeek get() = DayOfWeek.of(isoDayNumber)

/**
 * Converts a [java.time.DayOfWeek] into an [MpDayOfWeek].
 */
val DayOfWeek.mp: MpDayOfWeek get() = MpDayOfWeek.of(value)
