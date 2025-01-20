package de.peekandpoke.ultra.common.datetime

import common.datetime.DateTimeRangeConverter
import de.peekandpoke.ultra.common.ComparableTo
import kotlinx.serialization.Serializable

@Serializable
// TODO: test all of me
data class MpClosedLocalDateRange(
    val from: MpLocalDate,
    val to: MpLocalDate,
) : ComparableTo<MpDatePeriod> {
    companion object {
        /**
         * A range from [MpLocalDate.Genesis] until [MpLocalDate.Doomsday]
         */
        val forever: MpClosedLocalDateRange =
            MpClosedLocalDateRange(MpLocalDate.Genesis, MpLocalDate.Doomsday)

        /**
         * Creates a range for the given [from] and [to].
         */
        fun of(from: MpLocalDate, to: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = from, to = to)

        /**
         * Creates a range from [MpLocalDate.Genesis] until including [to]
         */
        fun endingAt(to: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = forever.from, to = to)

        /**
         * Creates a DateRange from including [from] until [MpLocalDate.Doomsday]
         */
        fun beginningAt(from: MpLocalDate): MpClosedLocalDateRange =
            MpClosedLocalDateRange(from = from, to = forever.to)
    }

    // TODO: test me
    @Serializable
    data class Partial(
        val from: MpLocalDate?,
        val to: MpLocalDate?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        fun asValidRange(): MpClosedLocalDateRange? = when (from != null && to != null) {
            true -> MpClosedLocalDateRange(from = from, to = to).takeIf { it.isValid }
            false -> null
        }
    }

    val asOpenRange: MpLocalDateRange by lazy {
        MpLocalDateRange(from = from, to = to.plusDays(1))
    }

    val asDatePeriod: MpDatePeriod by lazy { asOpenRange.asDatePeriod }

    val numberOfDays: Int by lazy { asOpenRange.numberOfDays }

    val numberOfNights: Int by lazy { asOpenRange.numberOfNights }

    val hasStart: Boolean get() = asOpenRange.hasStart

    val hasEnd: Boolean get() = asOpenRange.hasEnd

    val isOpen: Boolean get() = asOpenRange.isOpen

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = asOpenRange.isValid

    val isNotValid: Boolean get() = !isValid

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    fun asListOfDates(): List<MpLocalDate> = asOpenRange.asListOfDates()

    override fun compareTo(other: MpDatePeriod): Int {
        return asOpenRange.compareTo(other)
    }

    fun toZonedTimeRange(timezone: MpTimezone) = DateTimeRangeConverter(
        timezone = timezone,
        from = from,
        to = to,
    )

    // TODO: Test me
    fun contains(date: MpLocalDate): Boolean {
        return date in from..to
    }

    // TODO: Test me
    fun contains(other: MpClosedLocalDateRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    // TODO: Test me
    fun intersects(other: MpClosedLocalDateRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }
}
