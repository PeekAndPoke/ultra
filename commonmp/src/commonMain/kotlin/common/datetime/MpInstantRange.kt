package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("Detekt:TooManyFunctions")
@Serializable
data class MpInstantRange(
    val from: MpInstant,
    val to: MpInstant
) {
    companion object {
        /**
         * Range from [MpInstant.Genesis] until [MpInstant.Doomsday]
         */
        val forever = MpInstantRange(MpInstant.Genesis, MpInstant.Doomsday)

        /**
         * Creates an [MpInstantRange] starting at [from] with the given [duration].
         */
        fun of(from: MpInstant, duration: Duration): MpInstantRange = MpInstantRange(
            from = from,
            to = from.plus(duration),
        )

        /**
         * Creates an [MpInstantRange] from [from] until [MpInstant.Doomsday].
         */
        fun beginningAt(from: MpInstant): MpInstantRange = MpInstantRange(from, forever.to)

        /**
         * Creates an [MpInstantRange] from [MpInstant.Genesis] until [to].
         */
        fun endingAt(to: MpInstant): MpInstantRange = MpInstantRange(forever.from, to)
    }

    /**
     * The [Duration] of the range.
     */
    val duration: Duration get() = to - from

    /**
     * The range has a start when [from] is after [MpInstant.Genesis].
     */
    val hasStart: Boolean get() = from > MpInstant.Genesis

    /**
     * The range has an end when [to] is before [MpInstant.Doomsday].
     */
    val hasEnd: Boolean get() = to < MpInstant.Doomsday

    /**
     * The range is open, when it has no start, no end, or both.
     */
    val isOpen: Boolean get() = !isNotOpen

    /**
     * The range not is open, when it has a start and an end
     */
    val isNotOpen: Boolean get() = hasStart && hasEnd

    /**
     * The range is valid when [from] is before [to]
     */
    val isValid: Boolean get() = from < to

    /**
     * Converts into an [MpZonedDateTimeRange] at the given [timezone].
     */
    fun atZone(timezone: TimeZone): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = from.atZone(timezone),
            to = to.atZone(timezone),
        )
    }

    /**
     * Converts into an [MpZonedDateTimeRange] at the given [timezone].
     */
    fun atZone(timezone: MpTimezone): MpZonedDateTimeRange {
        return atZone(timezone.kotlinx)
    }

    /**
     * Converts into an [MpZonedDateTimeRange] at the systems default timezone.
     */
    fun atSystemDefaultZone(): MpZonedDateTimeRange {
        return atZone(TimeZone.currentSystemDefault())
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
        return isValid && datetime.toInstant() >= from && datetime.toInstant() < to
    }

    /**
     * Checks if this range contains the given [other] range.
     *
     * Returns `true` when all conditions are true:
     * 1. this range is valid
     * 2. the other range is valid
     * 3. this [from] <= the others [from]
     * 4. this [to] >= the others [to]
     */
    fun contains(other: MpInstantRange): Boolean {
        return (isValid && other.isValid) &&
                (from <= other.from && to >= other.to)
    }

    // TODO: Test
    fun intersects(other: MpInstantRange): Boolean {
        return (isValid && other.isValid) && (
                (other.from >= from && other.from < to) ||
                        (other.to > from && other.to <= to) ||
                        contains(other) ||
                        other.contains(this)
                )
    }

    /**
     * Adds the [duration] in absolute terms.
     */
    // TODO: Test
    fun plus(duration: Duration): MpInstantRange {
        return MpInstantRange(
            from = from.plus(duration),
            to = to.plus(duration),
        )
    }

    /**
     * Adds once the [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in              2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun plus(unit: DateTimeUnit, timezone: TimeZone): MpInstantRange {
        return plus(value = 1, unit = unit, timezone = timezone)
    }

    /**
     * Adds the [value] times [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in              2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun plus(value: Int, unit: DateTimeUnit, timezone: TimeZone): MpInstantRange {
        return MpInstantRange(
            from = from.plus(value = value, unit = unit, timezone = timezone),
            to = to.plus(value = value, unit = unit, timezone = timezone),
        )
    }

    /**
     * Subtracts the [duration] in absolute terms.
     */
    // TODO: Test
    fun minus(duration: Duration): MpInstantRange {
        return plus(duration.unaryMinus())
    }

    /**
     * Subtracts once the [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                   2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    // TODO: Test
    fun minus(unit: DateTimeUnit, timezone: TimeZone): MpInstantRange {
        return minus(1, unit, timezone)
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
    fun minus(value: Int, unit: DateTimeUnit, timezone: TimeZone): MpInstantRange {
        return plus(-value, unit, timezone)
    }
}
