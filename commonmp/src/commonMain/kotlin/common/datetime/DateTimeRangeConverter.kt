package common.datetime

import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalTime
import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.common.datetime.MpZonedDateTimeRange
import kotlinx.datetime.DateTimeUnit

class DateTimeRangeConverter(
    private val timezone: MpTimezone,
    private val from: MpLocalDate,
    private val to: MpLocalDate,
) {
    val fromNoonToNoon: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
        create(
            from = from.atStartOfDay(timezone).plus(12, DateTimeUnit.HOUR),
            to = to.atStartOfDay(timezone).plus(12, DateTimeUnit.HOUR),
        )
    }

    val fromMorningToEvening: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
        create(
            from = from.atStartOfDay(timezone),
            to = to.atStartOfDay(timezone).plus(1, DateTimeUnit.DAY),
        )
    }

    fun fromHourToHour(fromHour: Int, toHour: Int): MpZonedDateTimeRange {
        return create(
            from = from.atStartOfDay(timezone).plus(fromHour, DateTimeUnit.HOUR),
            to = to.atStartOfDay(timezone).plus(toHour, DateTimeUnit.HOUR),
        )
    }

    fun fromTimeToTime(fromTime: MpLocalTime, toTime: MpLocalTime): MpZonedDateTimeRange {
        return create(
            from = from.atTime(fromTime, timezone),
            to = to.atTime(toTime, timezone),
        )
    }

    private fun create(from: MpZonedDateTime, to: MpZonedDateTime): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = maxOf(MpZonedDateTime.Genesis, from),
            to = minOf(MpZonedDateTime.Doomsday, to),
        )
    }
}
