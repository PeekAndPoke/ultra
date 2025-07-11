package de.peekandpoke.ultra.common.datetime

import de.peekandpoke.ultra.common.ComparableTo
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

    val duration: Duration by lazy { to - from }

    val hasStart: Boolean get() = from > MpZonedDateTime.Genesis

    val hasEnd: Boolean get() = to < MpZonedDateTime.Doomsday

    val isOpen: Boolean get() = !hasStart || !hasEnd

    val isNotOpen: Boolean get() = !isOpen

    val isValid: Boolean get() = from < to

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

    fun contains(datetime: MpZonedDateTime): Boolean {
        return contains(datetime.toInstant())
    }

    fun contains(date: MpLocalDateTime, timezone: TimeZone): Boolean {
        return contains(date.toInstant(timezone))
    }

    fun contains(instant: MpInstant): Boolean {
        return instant >= from.toInstant() && instant < to.toInstant()
    }

    fun contains(other: MpZonedDateTimeRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    fun intersects(other: MpZonedDateTimeRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
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
