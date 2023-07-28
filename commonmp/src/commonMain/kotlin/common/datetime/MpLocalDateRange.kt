package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.ComparableTo
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
// TODO: test all of me
data class MpLocalDateRange(
    val from: MpLocalDate,
    val to: MpLocalDate,
) : ComparableTo<MpDatePeriod> {
    companion object {
        /**
         * A range from [MpLocalDate.Genesis] until [MpLocalDate.Doomsday]
         */
        val forever: MpLocalDateRange = MpLocalDateRange(MpLocalDate.Genesis, MpLocalDate.Doomsday)

        /**
         * Creates a range for the given [from] and [to].
         */
        fun of(from: MpLocalDate, to: MpLocalDate): MpLocalDateRange = MpLocalDateRange(from = from, to = to)

        /**
         * Creates a range from [MpLocalDate.Genesis] until including [to]
         */
        fun endingAt(to: MpLocalDate): MpLocalDateRange = MpLocalDateRange(from = forever.from, to = to)

        /**
         * Creates a DateRange from including [from] until [MpLocalDate.Doomsday]
         */
        fun beginningAt(from: MpLocalDate): MpLocalDateRange = MpLocalDateRange(from = from, to = forever.to)
    }

    @Serializable
    data class Partial(
        val from: MpLocalDate?,
        val to: MpLocalDate?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        fun asValidRange(): MpLocalDateRange? = when (from != null && to != null) {
            true -> MpLocalDateRange(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
    }

    inner class ToZonedDateTimeRangeConverter(private val timezone: MpTimezone) {

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

    val asDatePeriod: MpDatePeriod by lazy {
        if (isNotValid) {
            return@lazy MpDatePeriod.Zero
        }

        var years = 0
        var months = 0
        var days = 0

        var current = from

        val items = listOf(
            DateTimeUnit.YEAR to { ++years },
            DateTimeUnit.MONTH to { ++months },
            DateTimeUnit.DAY to { ++days },
        )

        items.forEach { (unit, action) ->
            while (true) {
                val next = current.plus(unit)

                if (next <= to) {
                    action()
                    current = next
                } else {
                    break
                }
            }
        }

        MpDatePeriod(years = years, months = months, days = days)
    }

    val asWholeDays: Int by lazy {
        if (isNotValid) {
            return@lazy 0
        }

        val seconds = toZonedTimeRange(MpTimezone.UTC).fromNoonToNoon.duration.inWholeSeconds

        round(seconds / (60 * 60 * 24.0)).toInt()
    }

    val hasStart: Boolean get() = from > MpLocalDate.Genesis

    val hasEnd: Boolean get() = to < MpLocalDate.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    val isNotValid: Boolean get() = !isValid

    override fun compareTo(other: MpDatePeriod): Int {
        return to.compareTo(from.plus(other))
    }

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    fun toZonedTimeRange(timezone: MpTimezone) = ToZonedDateTimeRangeConverter(timezone)
}
