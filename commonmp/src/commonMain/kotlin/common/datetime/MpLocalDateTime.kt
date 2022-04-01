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

@Serializable(with = MpLocalDateTimeSerializer::class)
data class MpLocalDateTime(val value: LocalDateTime) : Comparable<MpLocalDateTime> {

    companion object {
        fun of(
            year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, nanosecond: Int = 0
        ): MpLocalDateTime = MpLocalDateTime(
            LocalDateTime(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
                hour = hour,
                minute = minute,
                second = second,
                nanosecond = nanosecond,
            )
        )

        fun of(
            year: Int, month: Month, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, nanosecond: Int = 0
        ): MpLocalDateTime = of(
            year = year,
            month = month.number,
            day = day,
            hour = hour,
            minute = minute,
            second = second,
            nanosecond = nanosecond,
        )


        fun parse(isoString: String): MpLocalDateTime = MpLocalDateTime(
            value = LocalDateTime.parse(isoString),
        )

        val Genesis: MpLocalDateTime = MpLocalDateTime(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP).toLocalDateTime(TimeZone.UTC)
        )

        val Doomsday: MpLocalDateTime = MpLocalDateTime(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP).toLocalDateTime(TimeZone.UTC)
        )
    }

    val year: Int get() = value.year
    val monthNumber: Int get() = value.monthNumber
    val month: Month get() = value.month
    val dayOfMonth: Int get() = value.dayOfMonth
    val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    val dayOfYear: Int get() = value.dayOfYear

    val hour: Int get() = value.hour
    val minute: Int get() = value.minute
    val second: Int get() = value.second
    val nanosecond: Int get() = value.nanosecond

    override fun compareTo(other: MpLocalDateTime): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value.toString()
    }

    fun toInstant(timezone: TimeZone): MpInstant = MpInstant(
        value = value.toInstant(timezone)
    )

    fun atZone(timezone: TimeZone): MpZonedDateTime = toInstant(timezone).atZone(timezone)

    fun atUTC(): MpZonedDateTime = atZone(TimeZone.UTC)
}
