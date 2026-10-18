@file:JvmName("ConversionKotlinx")

package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.jvm.JvmName

/** Converts an [MpInstant] into a kotlinx [Instant]. */
val MpInstant.kotlinx: Instant get() = value

/** Converts a kotlinx [Instant] into an [MpInstant]. */
val Instant.mp: MpInstant get() = MpInstant(this)

/** Converts an [MpLocalDate] into a kotlinx [LocalDate]. */
val MpLocalDate.kotlinx: LocalDate get() = value

/** Converts a kotlinx [LocalDate] into an [MpLocalDate]. */
val LocalDate.mp: MpLocalDate get() = MpLocalDate(this)

/** Converts an [MpLocalDateTime] into a kotlinx [LocalDateTime]. */
val MpLocalDateTime.kotlinx: LocalDateTime get() = value

/** Converts a kotlinx [LocalDateTime] into an [MpLocalDateTime]. */
val LocalDateTime.mp: MpLocalDateTime get() = MpLocalDateTime(this)

/** Converts an [MpLocalTime] into a kotlinx [LocalTime]. */
val MpLocalTime.kotlinx: LocalTime get() = LocalTime.fromMillisecondOfDay(inWholeMilliSeconds().toInt())

/** Converts a kotlinx [LocalTime] into an [MpLocalTime], truncated to milliseconds. */
val LocalTime.mp: MpLocalTime get() = MpLocalTime.ofMilliSeconds(toMillisecondOfDay().toLong())

/** Converts an [MpDatePeriod] into a kotlinx [DatePeriod], which normalises months into years. */
val MpDatePeriod.kotlinx: DatePeriod get() = DatePeriod(years = years, months = months, days = days)

/** Converts a kotlinx [DatePeriod] into an [MpDatePeriod]. */
val DatePeriod.mp: MpDatePeriod get() = MpDatePeriod(years = years, months = months, days = days)

/** Converts an [MpDateTimePeriod] into a kotlinx [DateTimePeriod], which normalises the time components. */
val MpDateTimePeriod.kotlinx: DateTimePeriod
    get() = DateTimePeriod(
        years = years,
        months = months,
        days = days,
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        nanoseconds = milliseconds * 1_000_000L,
    )

/** Converts a kotlinx [DateTimePeriod] into an [MpDateTimePeriod], truncated to milliseconds. */
val DateTimePeriod.mp: MpDateTimePeriod get() = MpDateTimePeriod.of(this)
