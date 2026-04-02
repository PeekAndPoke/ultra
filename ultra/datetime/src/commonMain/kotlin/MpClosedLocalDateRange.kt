package io.peekandpoke.ultra.datetime

import kotlinx.serialization.Serializable

@Serializable
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

    fun contains(date: MpLocalDate): Boolean {
        return date in from..to
    }

    fun contains(other: MpClosedLocalDateRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: MpClosedLocalDateRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }

    fun intersects(other: MpLocalDateRange): Boolean {
        return asOpenRange.intersects(other)
    }

    /**
     * Returns `true` when this range touches or overlaps the [other] range.
     *
     * In other words, returns `true` when the union of both ranges is contiguous (has no gap).
     */
    fun touches(other: MpClosedLocalDateRange): Boolean {
        return asOpenRange.touches(other.asOpenRange)
    }

    /**
     * Returns `true` when this range is adjacent to the [other] range without overlapping.
     *
     * Two ranges are adjacent when one ends exactly where the other begins (the day after the last).
     */
    fun isAdjacentTo(other: MpClosedLocalDateRange): Boolean {
        return asOpenRange.isAdjacentTo(other.asOpenRange)
    }

    /**
     * Merges this range with the [other] one, by taking the minimal [from] and the maximal [to].
     *
     * If the ranges do not touch, returns both ranges sorted by [from].
     */
    fun mergeWith(other: MpClosedLocalDateRange): List<MpClosedLocalDateRange> {
        return asOpenRange.mergeWith(other.asOpenRange).map { it.asClosedRange }
    }

    /**
     * Cuts [other] from this range.
     *
     * Results in:
     * 1. An empty list, when other fully covers this range.
     * 2. A list with one entry, when other cuts at the left or the right side only.
     * 3. A list with two entries, when other lies in between this range.
     */
    fun cutAway(other: MpClosedLocalDateRange): List<MpClosedLocalDateRange> {
        return asOpenRange.cutAway(other.asOpenRange).map { it.asClosedRange }
    }
}
