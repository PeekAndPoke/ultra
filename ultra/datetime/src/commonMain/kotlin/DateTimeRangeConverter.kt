package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DateTimeUnit

class DateTimeRangeConverter(
    private val timezone: MpTimezone,
    private val from: MpLocalDate,
    private val to: MpLocalDate,
) {
    val fromNoonToNoon: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
        fromHourToHour(
            fromHour = 12,
            toHour = 12,
        )
    }

    val fromMorningToEvening: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
        create(
            from = from.atStartOfDay(timezone),
            to = to.atStartOfDay(timezone).plus(1, DateTimeUnit.DAY),
        )
    }

    fun fromHourToHour(fromHour: Int, toHour: Int): MpZonedDateTimeRange {
        return fromTimeToTime(
            fromTime = MpLocalTime.of(hour = fromHour, minute = 0),
            toTime = MpLocalTime.of(hour = toHour, minute = 0),
        )
    }

    fun fromTimeToTime(fromTime: MpLocalTime, toTime: MpLocalTime): MpZonedDateTimeRange {
        return create(
            from = from.atTime(fromTime, timezone),
            to = to.atTime(toTime, timezone),
        )
    }

    private fun create(from: MpZonedDateTime, to: MpZonedDateTime): MpZonedDateTimeRange {
        // Clamped by instant, not by MpZonedDateTime.compareTo (which also orders by local time and zone); on a tie
        // Genesis / Doomsday stay, as maxOf / minOf kept them before (ULTRA-00211).
        return MpZonedDateTimeRange(
            from = if (from.toInstant() > MpInstant.Genesis) from else MpZonedDateTime.Genesis,
            to = if (to.toInstant() < MpInstant.Doomsday) to else MpZonedDateTime.Doomsday,
        )
    }
}
