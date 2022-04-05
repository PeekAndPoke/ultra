package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpZonedDateTimeSerializer::class)
data class MpZonedDateTime private constructor(
    val datetime: MpLocalDateTime,
    val timezone: TimeZone
) : Comparable<MpZonedDateTime> {

    companion object {
        /**
         * Creates from a local [datetime] and a [timezone]
         */
        fun of(datetime: MpLocalDateTime, timezone: TimeZone): MpZonedDateTime {
            return MpZonedDateTime(
                datetime = datetime,
                timezone = when (timezone.id) {
                    // WHY? There seems to be a difference between TimeZone.UTC and TimeZone.of("UTC")
                    "UTC" -> TimeZone.UTC
                    else -> timezone
                }
            )
        }

        /**
         * Creates from the given epoch [millis] and [timezone].
         */
        fun fromEpochMillis(millis: Long, timezone: TimeZone): MpZonedDateTime {
            return MpInstant.fromEpochMillis(millis).atZone(timezone)
        }

        /**
         * Creates from the given epoch [seconds] and [timezone].
         */
        fun fromEpochSeconds(seconds: Long, timezone: TimeZone): MpZonedDateTime {
            return MpInstant.fromEpochSeconds(seconds).atZone(timezone)
        }

        /**
         * Parses from an [isoString] and a [timezone]
         */
        fun parse(isoString: String, timezone: TimeZone): MpZonedDateTime {
            return MpDateTimeParser.parseZonedDateTime(isoString).atZone(timezone)
        }

        /**
         * Parses from an [isoString] and tries to obtain the timezone from it.
         *
         * If there is no timezone in the [isoString] it will fall back to UTC.
         */
        fun parse(isoString: String): MpZonedDateTime = MpDateTimeParser.parseZonedDateTime(isoString)

        /**
         * The Genesis, a date in the distant past: -10000-01-01T00:00:00Z
         */
        val Genesis: MpZonedDateTime = MpInstant.Genesis.atZone(TimeZone.UTC)

        /**
         * The Doomsday, a date in the distant future: +10000-01-01T00:00:00Z
         */
        val Doomsday: MpZonedDateTime = MpInstant.Doomsday.atZone(TimeZone.UTC)
    }

    private val instant: MpInstant = datetime.toInstant(timezone)

    /** The year */
    val year: Int get() = datetime.year

    /** The month as number, where January is "1" */
    val monthNumber: Int get() = datetime.monthNumber

    /** The month */
    val month: Month get() = datetime.month

    /** The day of the month */
    val dayOfMonth: Int get() = datetime.dayOfMonth

    /** The hour */
    val hour: Int get() = datetime.hour

    /** The minute */
    val minute: Int get() = datetime.minute

    /** The second */
    val second: Int get() = datetime.second

    /** The nanosecond */
    val nanosecond: Int get() = datetime.nanosecond

    /** The day of the week */
    val dayOfWeek: DayOfWeek get() = datetime.dayOfWeek

    /** The day of the year */
    val dayOfYear: Int get() = datetime.dayOfYear

    /**
     * Compares to the [other].
     */
    override fun compareTo(other: MpZonedDateTime): Int {
        return instant.compareTo(other.instant)
    }

    /**
     * Converts to string.
     */
    override fun toString(): String {
        return "MpZonedDateTime(${toIsoString()})"
    }

    /**
     * Converts to an iso date string.
     */
    fun toIsoString(): String {

        fun Number.pad(n: Int = 2) = toString().padStart(n, '0')

        val tz = when (timezone.id) {
            "UTC", "Z" -> "Z"
            else -> "[$timezone]"
        }

        val yearStr = when {
            year > 9999 -> "+$year"
            else -> "$year"
        }

        return "$yearStr-${monthNumber.pad()}-${dayOfMonth.pad()}T" +
                "${hour.pad()}:${minute.pad()}:${second.pad()}." +
                (nanosecond / 1_000_000).pad(3) +
                tz
    }

    /**
     * Converts to an [MpInstant].
     */
    fun toInstant(): MpInstant = instant

    /**
     * Converts to an [MpLocalDate].
     */
    fun toLocalDate(): MpLocalDate = datetime.toDate()

    /**
     * Converts to an [MpLocalTime].
     */
    fun toLocalTime(): MpLocalTime = datetime.toTime()

    /**
     * Converts to an [MpLocalDateTime].
     */
    fun toLocalDateTime(): MpLocalDateTime = datetime

    /**
     * Gets the epoch millis.
     */
    fun toEpochMillis(): Long = toInstant().toEpochMillis()

    /**
     * Gets the epoch seconds.
     */
    fun toEpochSeconds(): Long = toInstant().toEpochSeconds()

    /**
     * Converts this date time into another [timezone].
     */
    fun atZone(timezone: TimeZone): MpZonedDateTime = toInstant().atZone(timezone)

    /**
     * Get the start of the year.
     */
    fun atStartOfYear(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, Month.JANUARY, 1)
    )

    /**
     * Get the start of the month.
     */
    fun atStartOfMonth(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, month, 1)
    )

    /**
     * Get the start of the day.
     */
    fun atStartOfDay(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, month, dayOfMonth)
    )

    /**
     * Get the start of the hour.
     */
    fun atStartOfHour(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour)
    )

    /**
     * Get the start of the minute.
     */
    fun atStartOfMinute(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour, minute)
    )

    /**
     * Get the start of the second.
     */
    fun atStartOfSecond(): MpZonedDateTime = copy(
        datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour, minute, second)
    )

    /**
     * Gets the start of the next upcoming [dayOfWeek].
     */
    fun atStartOfNext(dayOfWeek: DayOfWeek): MpZonedDateTime {

        var result = plus(1.days).atStartOfDay()

        while (result.dayOfWeek != dayOfWeek) {
            result = result.plus(1.days).atStartOfDay()
        }

        return result
    }

    /**
     * Gets the start of the next upcoming [dayOfWeek].
     */
    fun atStartOfPrevious(dayOfWeek: DayOfWeek): MpZonedDateTime {

        var result = atStartOfDay()

        while (result.dayOfWeek != dayOfWeek) {
            result = result.minus(1.days).atStartOfDay()
        }

        return result
    }

    /**
     * Add the given [duration] in absolute terms.
     */
    fun plus(duration: Duration): MpZonedDateTime {
        return toInstant().plus(duration).atZone(timezone)
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
    // TODO: Test me
    fun plus(unit: DateTimeUnit): MpZonedDateTime {
        return plus(1, unit)
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
    // TODO: Test me
    fun plus(value: Int, unit: DateTimeUnit): MpZonedDateTime {
        return toInstant().plus(value, unit, timezone).atZone(timezone)
    }

    /**
     * Subtracts the given duration in absolute terms.
     */
    fun minus(duration: Duration): MpZonedDateTime {
        return toInstant().minus(duration).atZone(timezone)
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
    // TODO: test me
    fun minus(unit: DateTimeUnit): MpZonedDateTime {
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
    // TODO: test me
    fun minus(value: Int, unit: DateTimeUnit): MpZonedDateTime {
        return plus(-value, unit)
    }

    /**
     * Calculates the [Duration] between this and the [other] date time.
     */
    operator fun minus(other: MpZonedDateTime): Duration {
        return toInstant() - other.toInstant()
    }
}
