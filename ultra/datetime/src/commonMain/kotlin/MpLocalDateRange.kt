package io.peekandpoke.ultra.datetime

import io.peekandpoke.ultra.common.ComparableTo
import io.peekandpoke.ultra.common.ComputedList
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.round

@Serializable
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

    /**
     * The number of days in this range. A range that reaches past [MpLocalDate.Doomsday] counts as open-ended and is
     * counted only up to Doomsday (the same at [MpLocalDate.Genesis]; ULTRA-00343). Pre-existing detail of that clamp:
     * it rounds a half day to the even number, so Doomsday itself is counted for every second start date.
     */
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

    /**
     * The dates of this range as a lazy view ([ComputedList]): each date is computed when read, nothing is stored.
     *
     * Cheap whatever the length (even for [forever]): `size`, `get`, `first`, `last` and iteration. `toList()`, `map`,
     * `filter` and the like build a real list of every date. Reads compute again: equal dates, not the same
     * instances. Read-only. Its length is [numberOfDays] (counted up to Doomsday); an invalid range gives an
     * empty list.
     */
    fun asListOfDates(): List<MpLocalDate> {
        if (isNotValid) {
            return emptyList()
        }

        return ComputedList(numberOfDays) { index -> from.plusDays(index) }
    }

    override fun compareTo(other: MpDatePeriod): Int {
        return to.compareTo(from.plus(other))
    }

    fun toZonedTimeRange(timezone: MpTimezone) = DateTimeRangeConverter(
        timezone = timezone,
        from = from,
        to = to.minusDays(1),
    )

    fun contains(date: MpLocalDate): Boolean {
        return date in from..<to
    }

    /**
     * Checks if this range fully contains the [other] range.
     */
    fun contains(other: MpLocalDateRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: MpLocalDateRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from in from..<to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }

    fun intersects(other: MpClosedLocalDateRange): Boolean {
        return intersects(other.asOpenRange)
    }

    /**
     * Returns `true` when this range touches or overlaps the [other] range.
     *
     * In other words, returns `true` when the union of both ranges is contiguous (has no gap).
     */
    fun touches(other: MpLocalDateRange): Boolean {
        return to >= other.from && from <= other.to
    }

    /**
     * Returns `true` when this range is adjacent to the [other] range without overlapping.
     *
     * Two ranges are adjacent when one ends exactly where the other begins.
     */
    fun isAdjacentTo(other: MpLocalDateRange): Boolean {
        return to == other.from || other.to == from
    }

    /**
     * Merges this range with the [other] one, by taking the minimal [from] and the maximal [to].
     *
     * If the ranges do not touch, returns both ranges sorted by [from].
     */
    fun mergeWith(other: MpLocalDateRange): List<MpLocalDateRange> = when {
        touches(other) -> listOf(
            MpLocalDateRange(
                from = minOf(from, other.from),
                to = maxOf(to, other.to),
            )
        )

        else -> listOf(this, other).sortedBy { it.from }
    }

    /**
     * Cuts [other] from this range.
     *
     * Results in:
     * 1. An empty list, when other fully covers this range.
     * 2. A list with one entry, when other cuts at the left or the right side only.
     * 3. A list with two entries, when other lies in between this range.
     */
    fun cutAway(other: MpLocalDateRange): List<MpLocalDateRange> {
        return when {
            this.isNotValid || other.isNotValid -> listOf(this)

            // all eaten up
            other.from <= from && to <= other.to -> emptyList()

            // the other one is inside and cuts the range into two pieces
            from < other.from && other.to < to -> listOf(
                MpLocalDateRange(from = from, to = other.from),
                MpLocalDateRange(from = other.to, to = to),
            )

            // eating away on the right side
            other.from < to && to <= other.to -> listOf(
                MpLocalDateRange(from = from, to = other.from),
            )

            // eating away on the left side
            other.from <= from && from < other.to -> listOf(
                MpLocalDateRange(from = other.to, to = to),
            )

            // Nothing happened
            else -> listOf(this)
        }
    }
}
