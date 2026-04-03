package io.peekandpoke.ultra.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("Detekt:TooManyFunctions")
@Serializable
data class MpZonedDateTimeRange(
    val from: MpZonedDateTime,
    val to: MpZonedDateTime,
) : ComparableTo<MpTemporalPeriod> {
    companion object {
        /**
         * Range from [MpZonedDateTime.Genesis] until [MpZonedDateTime.Doomsday]
         */
        val forever = MpZonedDateTimeRange(MpZonedDateTime.Genesis, MpZonedDateTime.Doomsday)

        /**
         * Creates an [MpZonedDateTimeRange] starting at [from] with the given [duration].
         */
        fun of(from: MpZonedDateTime, duration: Duration): MpZonedDateTimeRange = MpZonedDateTimeRange(
            from = from,
            to = from.plus(duration),
        )

        /**
         * Creates an [MpZonedDateTimeRange] starting at [from] with the given [duration].
         */
        fun of(from: MpZonedDateTime, period: MpTemporalPeriod): MpZonedDateTimeRange = MpZonedDateTimeRange(
            from = from,
            to = from.plus(period),
        )

        /**
         * Creates an [MpZonedDateTimeRange] from [from] until [MpZonedDateTime.Doomsday].
         */
        fun beginningAt(from: MpZonedDateTime): MpZonedDateTimeRange = MpZonedDateTimeRange(from, forever.to)

        /**
         * Creates an [MpZonedDateTimeRange] from [MpZonedDateTime.Genesis] until [to].
         */
        fun endingAt(to: MpZonedDateTime): MpZonedDateTimeRange = MpZonedDateTimeRange(forever.from, to)
    }

    @Serializable
    data class Partial(
        val from: MpZonedDateTime?,
        val to: MpZonedDateTime?,
    ) {
        companion object {
            val empty = Partial(from = null, to = null)
        }

        fun asValidRange(): MpZonedDateTimeRange? = when (from != null && to != null) {
            true -> MpZonedDateTimeRange(from = from, to = to).takeIf { it.isValid }
            false -> null
        }

        fun asDateRange() = MpClosedLocalDateRange.Partial(
            from = from?.toLocalDate(),
            to = to?.toLocalDate(),
        )
    }

    fun asPartialRange(): Partial {
        return Partial(from = from, to = to)
    }

    val duration: Duration by lazy { to - from }

    val hasStart: Boolean get() = from > MpZonedDateTime.Genesis

    val hasEnd: Boolean get() = to < MpZonedDateTime.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

    val isNotValid: Boolean get() = !isValid

    /**
     * Compares the duration to the given period.
     */
    override operator fun compareTo(other: MpTemporalPeriod): Int {
        return to.compareTo(from.plus(other))
    }

    /**
     * Converts to a [MpClosedLocalDateRange]
     */
    fun asDateRange(): MpClosedLocalDateRange = MpClosedLocalDateRange(
        from = from.toLocalDate(),
        to = to.toLocalDate(),
    )

    /**
     * Converts to the given [timezone].
     */
    fun atZone(timezone: TimeZone): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.atZone(timezone),
            to = to.atZone(timezone),
        )
    }

    fun atZone(timezone: MpTimezone): MpZonedDateTimeRange {
        return atZone(timezone.kotlinx)
    }

    /**
     * Checks if this range contains the given [datetime].
     *
     * Returns `true` when all conditions are true:
     * 1. the range is valid
     * 2. [datetime] >= [from]
     * 3. [datetime] < [to]
     */
    fun contains(datetime: MpAbsoluteDateTime): Boolean {
        val instant = datetime.toInstant()
        return isValid && instant in from.toInstant()..<to.toInstant()
    }

    /**
     * Checks if this range contains the given [datetime] at the given [timezone].
     */
    fun contains(datetime: MpLocalDateTime, timezone: TimeZone): Boolean {
        return contains(datetime.toInstant(timezone))
    }

    /**
     * Checks if this range fully contains the [other] range.
     */
    fun contains(other: MpZonedDateTimeRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: MpZonedDateTimeRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from in from..<to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }

    /**
     * Returns `true` when this range touches or overlaps the [other] range.
     *
     * In other words, returns `true` when the union of both ranges is contiguous (has no gap).
     */
    fun touches(other: MpZonedDateTimeRange): Boolean {
        return to >= other.from && from <= other.to
    }

    /**
     * Returns `true` when this range is adjacent to the [other] range without overlapping.
     *
     * Two ranges are adjacent when one ends exactly where the other begins.
     */
    fun isAdjacentTo(other: MpZonedDateTimeRange): Boolean {
        return to == other.from || other.to == from
    }

    /**
     * Merges this range with the [other] one, by taking the minimal [from] and the maximal [to].
     *
     * If the ranges do not touch, returns both ranges sorted by [from].
     */
    fun mergeWith(other: MpZonedDateTimeRange): List<MpZonedDateTimeRange> = when {
        touches(other) -> listOf(
            MpZonedDateTimeRange(
                from = minOf(from, other.from),
                to = maxOf(to, other.to),
            )
        )

        else -> listOf(this, other).sortedBy { it.from }
    }

    /**
     * Converts to a [MpInstantRange].
     */
    fun toInstantRange(): MpInstantRange {
        return MpInstantRange(
            from = from.toInstant(),
            to = to.toInstant()
        )
    }

    /**
     * Cuts [other] from this range.
     *
     * Results in:
     * 1. An empty list, when other fully cover this range.
     * 2. A list with one entry, when other cuts at the left or the right side only.
     * 3. A list with two entries, when other lies in between this range.
     */
    fun cutAway(other: MpZonedDateTimeRange): List<MpZonedDateTimeRange> {
        return toInstantRange().cutAway(other.toInstantRange())
            .map { it.atZone(from.timezone) }
    }

    /**
     * Adds the [duration] in absolute terms.
     */
    fun plus(duration: Duration): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.plus(duration),
            to = to.plus(duration),
        )
    }

    /**
     * Adds once the [unit] by taking the timezones into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example, adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in               2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun plus(unit: DateTimeUnit): MpZonedDateTimeRange {
        return plus(value = 1, unit = unit)
    }

    /**
     * Adds the [value] times [unit] by taking the timezones into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example, adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in               2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun plus(value: Int, unit: DateTimeUnit): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.plus(value = value, unit = unit),
            to = to.plus(value = value, unit = unit),
        )
    }

    /**
     * Subtracts the [duration] in absolute terms.
     */
    fun minus(duration: Duration): MpZonedDateTimeRange {
        return plus(duration.unaryMinus())
    }

    /**
     * Subtracts once the [unit] by taking the timezones into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example, subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                    2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun minus(unit: DateTimeUnit): MpZonedDateTimeRange {
        return minus(1, unit)
    }

    /**
     * Subtracts the [value] times [unit].
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example, subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                    2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun minus(value: Int, unit: DateTimeUnit): MpZonedDateTimeRange {
        return plus(-value, unit)
    }
}

fun MpZonedDateTimeRange.formatDdMmmYyyyHhMm(): String {
    if (from.atStartOfDay() == to.atStartOfDay()) {
        return "${from.formatDdMmmYyyy()} ${from.formatHhMm()} - ${to.formatHhMm()}"
    }

    return "${from.formatDdMmmYyyyHhMm()} - ${to.formatDdMmmYyyyHhMm()}"
}
