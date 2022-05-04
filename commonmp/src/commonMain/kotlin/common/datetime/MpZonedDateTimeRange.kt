package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("Detekt:TooManyFunctions")
@Serializable
data class MpZonedDateTimeRange(
    val from: MpZonedDateTime,
    val to: MpZonedDateTime
) {
    companion object {
        /**
         * Range from [MpZonedDateTime.Genesis] until [MpZonedDateTime.Doomsday]
         */
        // TODO: test me
        val forever = MpZonedDateTimeRange(MpZonedDateTime.Genesis, MpZonedDateTime.Doomsday)

        /**
         * Creates an [MpZonedDateTimeRange] starting at [from] with the given [duration].
         */
        // TODO: test me
        fun of(from: MpZonedDateTime, duration: Duration): MpZonedDateTimeRange = MpZonedDateTimeRange(
            from = from,
            to = from.plus(duration),
        )

        /**
         * Creates an [MpZonedDateTimeRange] starting at [from] with the given [duration].
         */
        // TODO: test me
        fun of(from: MpZonedDateTime, period: MpTemporalPeriod): MpZonedDateTimeRange = MpZonedDateTimeRange(
            from = from,
            to = from.plus(period),
        )

        /**
         * Creates an [MpZonedDateTimeRange] from [from] until [MpZonedDateTime.Doomsday].
         */
        // TODO: test me
        fun beginningAt(from: MpZonedDateTime): MpZonedDateTimeRange = MpZonedDateTimeRange(from, forever.to)

        /**
         * Creates an [MpZonedDateTimeRange] from [MpZonedDateTime.Genesis] until [to].
         */
        // TODO: test me
        fun endingAt(to: MpZonedDateTime): MpZonedDateTimeRange = MpZonedDateTimeRange(forever.from, to)
    }

    // TODO: Test
    val duration: Duration get() = to - from

    // TODO: Test
    val hasStart: Boolean get() = from > MpZonedDateTime.Genesis

    // TODO: Test
    val hasEnd: Boolean get() = to < MpZonedDateTime.Doomsday

    // TODO: Test
    val isOpen: Boolean get() = !hasStart || !hasEnd

    // TODO: Test
    val isNotOpen: Boolean get() = !isOpen

    // TODO: Test
    val isValid: Boolean get() = from < to

    // TODO: Test
    fun contains(datetime: MpZonedDateTime): Boolean {
        return contains(datetime.toInstant())
    }

    // TODO: Test
    fun contains(date: MpLocalDateTime, timezone: TimeZone): Boolean {
        return contains(date.toInstant(timezone))
    }

    // TODO: Test
    fun contains(instant: MpInstant): Boolean {
        return instant >= from.toInstant() && instant < to.toInstant()
    }

    // TODO: Test
    fun contains(other: MpZonedDateTimeRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    // TODO: Test
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
    // TODO: Test
    fun toInstantRange(): MpInstantRange {
        return MpInstantRange(
            from = from.toInstant(),
            to = to.toInstant()
        )
    }

    /**
     * Converts to the given [timezone].
     */
    // TODO: Test
    fun atZone(timezone: TimeZone): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.atZone(timezone),
            to = to.atZone(timezone),
        )
    }

    /**
     * Adds the [duration] in absolute terms.
     */
    // TODO: Test
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
     * For example adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in              2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun plus(unit: DateTimeUnit): MpZonedDateTimeRange {
        return plus(value = 1, unit = unit)
    }

    /**
     * Adds the [value] times [unit] by taking the timezones into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in              2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun plus(value: Int, unit: DateTimeUnit): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.plus(value = value, unit = unit),
            to = to.plus(value = value, unit = unit),
        )
    }

    /**
     * Subtracts the [duration] in absolute terms.
     */
    // TODO: Test
    fun minus(duration: Duration): MpZonedDateTimeRange {
        return plus(duration.unaryMinus())
    }

    /**
     * Subtracts once the [unit] by taking the timezones into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                   2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun minus(unit: DateTimeUnit): MpZonedDateTimeRange {
        return minus(1, unit)
    }

    /**
     * Subtracts the [value] times [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                   2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun minus(value: Int, unit: DateTimeUnit): MpZonedDateTimeRange {
        return plus(-value, unit)
    }
}

// TODO: test me
fun MpZonedDateTimeRange.formatDdMmmYyyyHhMm(): String {
    if (from.atStartOfDay() == to.atStartOfDay()) {
        return "${from.formatDdMmmYyyy()} ${from.formatHhMm()} - ${to.formatHhMm()}"
    }

    return "${from.formatDdMmmYyyyHhMm()} - ${to.formatDdMmmYyyyHhMm()}"
}
