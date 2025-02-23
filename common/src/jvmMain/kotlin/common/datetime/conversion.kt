@file:JvmName("ConversionJvm")

package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
// TODO: test me
val OffsetDateTime.mp get(): MpInstant = toInstant().mp

/**
 * Converts a [java.time.OffsetDateTime] into an [MpZonedDateTime]
 */
// TODO: test me
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
        return ZonedDateTime.of(datetime.jvm, ZoneId.of(timezone.id))
    }

/**
 * Converts a [java.time.ZonedDateTime] into an [MpZonedDateTime]
 */
val ZonedDateTime.mp: MpZonedDateTime
    get() {
        return MpZonedDateTime.of(
            datetime = toLocalDateTime().mp,
            timezone = zone.mp.kotlinx,
        )
    }

/**
 * Converts a [java.time.LocalTime] into an [MpLocalTime]
 */
// TODO: test me
val LocalTime.mp: MpLocalTime
    get() =
        MpLocalTime.ofMilliSeconds(milliSecondsOfDay = 1000L * (hour * 60 * 60 + minute * 60 + second))

/**
 * Converts an [MpLocalTime] into a [java.time.LocalDate]
 */
// TODO: test me
val MpLocalTime.jvm get(): LocalTime = LocalTime.ofSecondOfDay(inWholeMilliSeconds() / 1000)

/**
 * Converts a [java.time.ZoneId] to a [TimeZone].
 */
// TODO: test me
val ZoneId.kotlinx: TimeZone get() = TimeZone.of(id)

/**
 * Converts a [java.time.ZoneId] to a [TimeZone].
 */
// TODO: test me
fun TimeZone.Companion.of(zone: ZoneId): TimeZone = of(zone.id)

/**
 * Converts a [TimeZone] into a [java.time.ZoneId].
 */
// TODO: test me
val TimeZone.jvm: ZoneId get() = ZoneId.of(id)

/**
 * Converts [java.time.ZoneId] into an [MpTimezone].
 */
// TODO: test me
val ZoneId.mp: MpTimezone get() = MpTimezone.of(id)

/**
 * Converts an [MpTimezone] into a [java.time.ZoneId]
 */
// TODO: test me
val MpTimezone.jvm: ZoneId get() = ZoneId.of(id)
