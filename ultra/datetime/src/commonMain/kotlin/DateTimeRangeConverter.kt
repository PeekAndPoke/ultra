package io.peekandpoke.ultra.datetime

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
            // the start of the next day: 'start of the last day plus one day' ends an hour late where that
            // day's own midnight is in a DST gap (ULTRA-00341)
            to = to.atTime(MpLocalTime.EndOfDay, timezone),
        )
    }

    /**
     * The range from [fromHour] on the first day to [toHour] on the last. A [toHour] of 24 is [MpLocalTime.EndOfDay],
     * the start of the next day; [fromHour] wraps (24 is 00:00).
     */
    fun fromHourToHour(fromHour: Int, toHour: Int): MpZonedDateTimeRange {
        return fromTimeToTime(
            fromTime = MpLocalTime.of(hour = fromHour, minute = 0),
            toTime = if (toHour == 24) MpLocalTime.EndOfDay else MpLocalTime.of(hour = toHour, minute = 0),
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
