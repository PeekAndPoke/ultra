package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

// TODO: test all of me
@Suppress("Detekt.LongParameterList")
@Serializable(with = MpLocalDateTimeSerializer::class)
data class MpLocalDateTime internal constructor(internal val value: LocalDateTime) : Comparable<MpLocalDateTime> {

    companion object {
        fun of(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliSecond: Int = 0,
        ): MpLocalDateTime = MpLocalDateTime(
            LocalDateTime(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
                hour = hour,
                minute = minute,
                second = second,
                nanosecond = milliSecond * 1_000_000,
            )
        )

        fun of(
            year: Int,
            month: Month,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            milliSecond: Int = 0,
        ): MpLocalDateTime = of(
            year = year,
            month = month.number,
            day = day,
            hour = hour,
            minute = minute,
            second = second,
            milliSecond = milliSecond,
        )

        // TODO: Test me
        fun of(date: MpLocalDate, time: MpLocalTime): MpLocalDateTime = of(
            year = date.year,
            month = date.month,
            day = date.day,
            hour = time.hour,
            minute = time.minute,
            second = time.second,
            milliSecond = time.milliSecond,
        )

        /**
         * Parses an [MpLocalDateTime] from the given [isoString].
         *
         * Throws an [IllegalArgumentException] if parsing fails.
         */
        @Throws(IllegalArgumentException::class)
        fun parse(isoString: String): MpLocalDateTime {
            return try {
                parseInternal(isoString)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse MpLocalDateTime from '$isoString'", e)
            }
        }

        /**
         * Parses an [MpLocalDateTime] from the given [isoString].
         *
         * Returns null if parsing fails.
         */
        fun tryParse(isoString: String): MpLocalDateTime? {
            return try {
                parseInternal(isoString)
            } catch (e: Throwable) {
                null
            }
        }

        private fun parseInternal(isoString: String): MpLocalDateTime {
            return MpDateTimeParser.parseInstant(isoString).atZone(TimeZone.UTC).datetime
        }

        val Genesis: MpLocalDateTime = MpLocalDateTime(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP).toLocalDateTime(TimeZone.UTC)
        )

        val Doomsday: MpLocalDateTime = MpLocalDateTime(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP).toLocalDateTime(TimeZone.UTC)
        )
    }

    /** The year */
    val year: Int get() = value.year

    /** The month as number, where January is "1" */
    val monthNumber: Int get() = value.monthNumber

    /** The month */
    val month: Month get() = value.month

    /** The day of the month */
    val dayOfMonth: Int get() = value.dayOfMonth

    /** The hour */
    val hour: Int get() = value.hour

    /** The minute */
    val minute: Int get() = value.minute

    /** The second */
    val second: Int get() = value.second

    /** The millisecond */
    val milliSecond: Int get() = value.nanosecond / 1_000_000

    // TODO: test me
    /** The day of the week */
    val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    // TODO: test me
    /** The day of the year */
    val dayOfYear: Int get() = value.dayOfYear

    override fun compareTo(other: MpLocalDateTime): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return "MpLocalDateTime(${toIsoString()})"
    }

    fun toIsoString(): String = atUTC().toIsoString()

    fun toDate(): MpLocalDate = MpLocalDate(value.date)

    fun toTime(): MpLocalTime = MpLocalTime.of(
        hour = hour,
        minute = minute,
        second = second,
        milliSecond = milliSecond,
    )

    fun toInstant(timezone: TimeZone): MpInstant = MpInstant(
        value = value.toInstant(timezone)
    )

    fun toInstant(timezone: MpTimezone): MpInstant = toInstant(timezone.kotlinx)

    fun atZone(timezone: TimeZone): MpZonedDateTime {
        return toInstant(timezone).atZone(timezone)
    }

    fun atZone(timezone: MpTimezone): MpZonedDateTime {
        return atZone(timezone.kotlinx)
    }

    fun atUTC(): MpZonedDateTime = atZone(TimeZone.UTC)
}
