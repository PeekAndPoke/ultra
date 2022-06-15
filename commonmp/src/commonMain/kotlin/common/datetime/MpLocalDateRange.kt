package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.ComparedTo
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable

@Serializable
// TODO: test all of me
data class MpLocalDateRange(
    val from: MpLocalDate,
    val to: MpLocalDate,
) : ComparedTo<MpDatePeriod> {
    companion object {
        /**
         * A range from [MpLocalDate.Genesis] until [MpLocalDate.Doomsday]
         */
        val forever: MpLocalDateRange = MpLocalDateRange(MpLocalDate.Genesis, MpLocalDate.Doomsday)

        /**
         * Creates a range from [GenesisLocalDate] until including [to]
         */
        fun endingAt(to: MpLocalDate): MpLocalDateRange = MpLocalDateRange(from = forever.from, to = to)

        /**
         * Creates a DateRange from including [from] until [DoomsdayLocalDate]
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

    inner class ToZonedDateTimeRangeConverter(timezone: MpTimezone) {
        val fromNoonToNoon: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
            MpZonedDateTimeRange(
                from = maxOf(
                    MpZonedDateTime.Genesis,
                    from.atStartOfDay(timezone).plus(12, DateTimeUnit.HOUR),
                ),
                to = minOf(
                    to.atStartOfDay(timezone).plus(12, DateTimeUnit.HOUR),
                    MpZonedDateTime.Doomsday,
                ),
            )
        }

        val fromMorningToEvening: MpZonedDateTimeRange by lazy(LazyThreadSafetyMode.NONE) {
            MpZonedDateTimeRange(
                from = maxOf(
                    MpZonedDateTime.Genesis,
                    from.atStartOfDay(timezone),
                ),
                to = minOf(
                    to.atStartOfDay(timezone).plus(1, DateTimeUnit.DAY),
                    MpZonedDateTime.Doomsday,
                ),
            )
        }
    }

    val hasStart: Boolean get() = from > MpLocalDate.Genesis

    val hasEnd: Boolean get() = to < MpLocalDate.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    override fun compareTo(other: MpDatePeriod): Int {
        return to.compareTo(from.plus(other))
    }

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    fun toZonedTimeRange(timezone: MpTimezone) = ToZonedDateTimeRangeConverter(timezone)
}
