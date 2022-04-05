package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpInstantSerializer::class)
data class MpInstant internal constructor(internal val value: Instant) : Comparable<MpInstant> {

    companion object {
        /**
         * Creates from the current system time.
         */
        fun now(): MpInstant = MpInstant(
            value = Clock.System.now(),
        )

        /**
         * Creates from the given epoch [millis].
         */
        fun fromEpochMillis(millis: Long): MpInstant = MpInstant(
            value = Instant.fromEpochMilliseconds(millis)
        )

        /**
         * Creates from the given epoch [seconds].
         */
        fun fromEpochSeconds(seconds: Long, nanosecondAdjustment: Number = 0): MpInstant = MpInstant(
            value = Instant.fromEpochSeconds(seconds, nanosecondAdjustment.toLong())
        )

        /**
         * Parses from the given [isoString].
         */
        fun parse(isoString: String): MpInstant = MpDateTimeParser.parseInstant(isoString)

        /**
         * The Genesis, a date in the distant past: -10000-01-01T00:00:00Z
         */
        val Genesis: MpInstant = MpInstant(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP)
        )

        /**
         * The Doomsday, a date in the distant future: +10000-01-01T00:00:00Z
         */
        val Doomsday: MpInstant = MpInstant(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP)
        )
    }

    /**
     * Compares.
     */
    override fun compareTo(other: MpInstant): Int {
        return value.compareTo(other.value)
    }

    /**
     * Creates a string representation.
     */
    override fun toString(): String {
        return "MpInstant(${toIsoString()})"
    }

    /**
     * Creates an iso date time string.
     */
    fun toIsoString(): String {
        return atZone(TimeZone.UTC).toIsoString()
    }

    /**
     * Gets the epoch milliseconds.
     */
    fun toEpochMillis(): Long {
        return value.toEpochMilliseconds()
    }

    /**
     * Gets the epoch seconds.
     */
    fun toEpochSeconds(): Long {
        return value.epochSeconds
    }

    /**
     * Create a [MpZonedDateTime] at the given [timezone]
     */
    fun atZone(timezone: TimeZone): MpZonedDateTime {
        return MpZonedDateTime.of(
            MpLocalDateTime(value.toLocalDateTime(timezone)),
            timezone,
        )
    }

    /**
     * Adds the [duration] in absolute terms.
     */
    fun plus(duration: Duration): MpInstant {
        return MpInstant(value = value.plus(duration))
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
    fun plus(unit: DateTimeUnit, timezone: TimeZone): MpInstant {
        return plus(1, unit, timezone)
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
    fun plus(value: Int, unit: DateTimeUnit, timezone: TimeZone): MpInstant = MpInstant(
        value = this.value.plus(
            value = value,
            unit = unit,
            timeZone = timezone,
        )
    )

    /**
     * Subtracts the [duration] in absolute terms.
     */
    fun minus(duration: Duration): MpInstant {
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
    fun minus(unit: DateTimeUnit, timezone: TimeZone): MpInstant {
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
    fun minus(value: Int, unit: DateTimeUnit, timezone: TimeZone): MpInstant {
        return plus(-value, unit, timezone)
    }

    /**
     * Calculates the [Duration] between this and another instant
     */
    operator fun minus(other: MpInstant): Duration {
        return this.value - other.value
    }
}
