package de.peekandpoke.ultra.common.datetime

import common.datetime.DateTimeRangeConverter
import de.peekandpoke.ultra.common.ComparableTo
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable
import kotlin.math.max
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

    val asClosedRange: MpClosedLocalDateRange by lazy {
        MpClosedLocalDateRange(from = from, to = to.minusDays(1))
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

    val numberOfDays: Int by lazy {
        if (isNotValid) {
            return@lazy 0
        }

        val seconds = toZonedTimeRange(MpTimezone.UTC).fromNoonToNoon.duration.inWholeSeconds

        round(seconds / (60 * 60 * 24.0)).toInt() + 1
    }

    val numberOfNights: Int by lazy {
        max(0, numberOfDays - 1)
    }

    val hasStart: Boolean get() = from > MpLocalDate.Genesis

    val hasEnd: Boolean get() = to < MpLocalDate.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    val isNotValid: Boolean get() = !isValid

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    fun asListOfDates(): List<MpLocalDate> {
        if (isNotValid) {
            return emptyList()
        }

        return (0 until numberOfDays).map { from.plusDays(it) }
    }

    override fun compareTo(other: MpDatePeriod): Int {
        return to.compareTo(from.plus(other))
    }

    fun toZonedTimeRange(timezone: MpTimezone) = DateTimeRangeConverter(
        timezone = timezone,
        from = from,
        to = to.minusDays(1),
    )
}
