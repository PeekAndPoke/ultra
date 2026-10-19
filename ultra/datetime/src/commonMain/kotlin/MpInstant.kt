package io.peekandpoke.ultra.datetime

import io.peekandpoke.ultra.common.ComparableTo
import io.peekandpoke.ultra.common.slumber.Slumber
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Instant

@Suppress("Detekt:TooManyFunctions")
@Serializable(with = MpInstantSerializer::class)
@Slumber.As(MpDateTimeRawData::class)
data class MpInstant(
    internal val value: Instant,
) : MpAbsoluteDateTime, ComparableTo<MpInstant> {

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
         * Tries to parse from the given [isoString].
         *
         * If parsing fails null is returned.
         */
        fun tryParse(isoString: String): MpInstant? = try {
            parse(isoString)
        } catch (_: Throwable) {
            null
        }

        /**
         * Instant at the start of the unix epoch 1970-01-01T00:00:00.000Z
         */
        val Epoch: MpInstant = fromEpochMillis(0)

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
     * Compares to the [other] instant.
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
     * Returns itself
     */
    override fun toInstant(): MpInstant {
        return this
    }

    /**
     * Create a [MpZonedDateTime] at the given [timezone]
     */
    override fun atZone(timezone: TimeZone): MpZonedDateTime {
        return atZone(timezone = timezone.mp)
    }

    /**
     * Create a [MpZonedDateTime] at the given [timezone]
     */
    override fun atZone(timezone: MpTimezone): MpZonedDateTime {
        return MpZonedDateTime.of(
            MpLocalDateTime(TimezoneEngine.toLocalDateTime(timezone, value)),
            timezone,
        )
    }

    /**
     * Creates a [MpLocalDate] at the given timezone
     */
    fun toLocalDate(timezone: MpTimezone): MpLocalDate {
        return atZone(timezone).toLocalDate()
    }

    /**
     * Creates an iso date time string.
     */
    fun toIsoString(): String {
        return atZone(MpTimezone.UTC).toIsoString()
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
     * Creates a [MpInstantRange] with this as the start and the given [duration].
     */
    fun toRange(duration: Duration): MpInstantRange {
        return MpInstantRange.of(from = this, duration = duration)
    }

    /**
     * Creates a [MpZonedDateTimeRange] with this as the start and the given [period].
     */
    fun toRange(period: MpTemporalPeriod, timezone: MpTimezone): MpInstantRange {
        return MpInstantRange(from = this, to = plus(period, timezone).toInstant())
    }

    /**
     * Adds the [duration] in absolute terms.
     */
    operator fun plus(duration: Duration): MpInstant {
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
     * Adds once the [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example adding 1 Day to 2022-03-27T00:00:00[Europe/Berlin]
     * Will result in              2022-03-28T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun plus(unit: DateTimeUnit, timezone: MpTimezone): MpInstant {
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
    fun plus(value: Int, unit: DateTimeUnit, timezone: TimeZone): MpInstant {
        return plus(value = value, unit = unit, timezone = timezone.mp)
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
    fun plus(value: Int, unit: DateTimeUnit, timezone: MpTimezone): MpInstant {
        if (value == 0) {
            return this
        }

        return MpInstant(
            value = TimezoneEngine.plus(instant = this.value, value = value, unit = unit, timezone = timezone)
        )
    }

    /**
     * Adds the given [period] in the given [timezone].
     *
     * Each component is added individually starting with years, months, ..., milliseconds
     */
    fun plus(period: MpTemporalPeriod, timezone: MpTimezone): MpInstant {

        return plus(period.years, DateTimeUnit.YEAR, timezone)
            .plus(period.months, DateTimeUnit.MONTH, timezone)
            .plus(period.days, DateTimeUnit.DAY, timezone)
            .plus(period.hours, DateTimeUnit.HOUR, timezone)
            .plus(period.minutes, DateTimeUnit.MINUTE, timezone)
            .plus(period.seconds, DateTimeUnit.SECOND, timezone)
            .plus(period.milliseconds, DateTimeUnit.MILLISECOND, timezone)
    }

    /**
     * Subtracts the [duration] in absolute terms.
     */
    operator fun minus(duration: Duration): MpInstant {
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
     * Subtracts once the [unit] by taking the [timezone] into account.
     *
     * This method is useful for timezones where there is a DST (Daylight Saving Transition).
     *
     * For example subtracting 1 Day to 2022-03-28T00:00:00[Europe/Berlin]
     * Will result in                   2022-03-27T00:00:00[Europe/Berlin]
     * while the difference between both instants is only 23 hours.
     */
    fun minus(unit: DateTimeUnit, timezone: MpTimezone): MpInstant {
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
        return minus(value = value, unit = unit, timezone = timezone.mp)
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
    fun minus(value: Int, unit: DateTimeUnit, timezone: MpTimezone): MpInstant {
        return plus(-value, unit, timezone)
    }

    /**
     * Calculates the [Duration] between this and another instant
     */
    operator fun minus(other: MpInstant): Duration {
        return this.value - other.value
    }

    /**
     * Subtracts the given [period] in the given [timezone].
     *
     * Each component is added individually starting with years, months, ..., milliseconds
     */
    fun minus(period: MpTemporalPeriod, timezone: MpTimezone): MpInstant {
        return plus(-period, timezone)
    }
}
