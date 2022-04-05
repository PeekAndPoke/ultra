package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class MpInstantRange(
    val from: MpInstant,
    val to: MpInstant
) {
    companion object {
        // TODO: Test
        val forever = MpInstantRange(MpInstant.Genesis, MpInstant.Doomsday)

        // TODO: test me
        fun of(from: MpInstant, duration: Duration): MpInstantRange = MpInstantRange(
            from = from,
            to = from.plus(duration),
        )
    }

    // TODO: Test
    val duration: Duration get() = from - to

    // TODO: Test
    val hasStart: Boolean get() = from > MpInstant.Genesis

    // TODO: Test
    val hasEnd: Boolean get() = to < MpInstant.Doomsday

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
    fun contains(datetime: MpLocalDateTime, timezone: TimeZone): Boolean {
        return contains(datetime.toInstant(timezone))
    }

    // TODO: Test
    fun contains(instant: MpInstant): Boolean {
        return instant >= from && instant < to
    }

    // TODO: Test
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
     * Create a [MpZonedDateTime] at the given [timezone]
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
