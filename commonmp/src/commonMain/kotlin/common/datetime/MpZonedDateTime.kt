package de.peekandpoke.ultra.common.datetime

import com.soywiz.klock.DateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("DataClassPrivateConstructor", "Detekt:TooManyFunctions")
@Serializable(with = MpZonedDateTimeSerializer::class)
data class MpZonedDateTime private constructor(
    val datetime: MpLocalDateTime,
    val timezone: MpTimezone,
) : MpAbsoluteDateTime, Comparable<MpZonedDateTime> {

    companion object {
        /**
         * Creates from a local [datetime] and a [timezone]
         */
        fun of(datetime: MpLocalDateTime, timezone: TimeZone): MpZonedDateTime {
            return of(
                datetime = datetime,
                timezone = when (timezone.id) {
                    // WHY? There seems to be a difference between TimeZone.UTC and TimeZone.of("UTC")
                    "UTC" -> TimeZone.UTC
                    else -> timezone
                }.mp
            )
        }

        /**
         * Creates from a local [datetime] and a [timezone]
         */
        fun of(datetime: MpLocalDateTime, timezone: MpTimezone): MpZonedDateTime {
            return MpZonedDateTime(datetime = datetime, timezone = timezone)
        }

        /**
         * Creates from the given epoch [millis] and [timezone].
         */
        fun fromEpochMillis(millis: Long, timezone: TimeZone): MpZonedDateTime {
            return fromEpochMillis(millis = millis, timezone = timezone.mp)
        }

        /**
         * Creates from the given epoch [millis] and [timezone].
         */
        // TODO: test me
        fun fromEpochMillis(millis: Long, timezone: MpTimezone): MpZonedDateTime {
            return MpInstant.fromEpochMillis(millis).atZone(timezone)
        }

        /**
         * Creates from the given epoch [seconds] and [timezone].
         */
        fun fromEpochSeconds(seconds: Long, timezone: TimeZone): MpZonedDateTime {
            return fromEpochSeconds(seconds = seconds, timezone = timezone.mp)
        }

        /**
         * Creates from the given epoch [seconds] and [timezone].
         */
        // TODO: test me
        fun fromEpochSeconds(seconds: Long, timezone: MpTimezone): MpZonedDateTime {
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

    /** The millisecond */
    val milliSecond: Int get() = datetime.milliSecond

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
     * Converts into a human-readable string by the given [formatString].
     *
     * See https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html
     */
    fun format(formatString: String): String {
        val ts = toEpochMillis() + timezone.offsetMillisAt(instant)

        val klock = DateTime.fromUnix(ts)

        return klock.format(formatString)
    }

    /**
     * Converts to an iso date string.
     */
    fun toIsoString(): String {

        fun Number.pad(n: Int = 2) = toString().padStart(n, '0')

        val tz = when (val tzId = timezone.id) {
            "UTC", "Z" -> "Z"
            else -> "[${tzId}]"
        }

        val yearStr = when {
            year > 9999 -> "+$year"
            else -> "$year"
        }

        return "$yearStr-${monthNumber.pad()}-${dayOfMonth.pad()}T" +
                "${hour.pad()}:${minute.pad()}:${second.pad()}." +
                milliSecond.pad(3) +
                tz
    }

    /**
     * Converts to an [MpInstant].
     */
    override fun toInstant(): MpInstant = instant

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
     * Creates a [MpZonedDateTimeRange] with this as the start and the given [duration].
     */
    // TODO: test me
    fun toRange(duration: Duration): MpZonedDateTimeRange {
        return MpZonedDateTimeRange.of(from = this, duration = duration)
    }

    /**
     * Creates a [MpZonedDateTimeRange] with this as the start and the given [period].
     */
    // TODO: test me
    fun toRange(period: MpTemporalPeriod): MpZonedDateTimeRange {
        return MpZonedDateTimeRange.of(from = this, period = period)
    }

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
     * Gets the start of the upcoming [dayOfWeek].
     */
    fun atStartOfNext(dayOfWeek: DayOfWeek): MpZonedDateTime {

        var result = plus(1, DateTimeUnit.DAY).atStartOfDay()

        while (result.dayOfWeek != dayOfWeek) {
            result = result.plus(1, DateTimeUnit.DAY).atStartOfDay()
        }

        return result
    }

    /**
     * Gets the start of the preceding [dayOfWeek].
     */
    fun atStartOfPrevious(dayOfWeek: DayOfWeek): MpZonedDateTime {

        var result = atStartOfDay()

        while (result.dayOfWeek != dayOfWeek) {
            result = result.minus(1, DateTimeUnit.DAY).atStartOfDay()
        }

        return result
    }

    /**
     * Sets the time of the day to the given [time].
     */
    // TODO: test me
    fun atTime(time: MpLocalTime): MpZonedDateTime {
        return MpLocalDateTime.of(
            date = toLocalDate(),
            time = time
        ).atZone(timezone = timezone)
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
     * Adds the given [period] in the [timezone].
     *
     * Each component is added individually starting with years, months, ..., milliseconds
     */
    // TODO: test me
    fun plus(period: MpTemporalPeriod): MpZonedDateTime {
        return toInstant().plus(period, timezone).atZone(timezone)
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

    /**
     * Subtracts the given [period] in the [timezone].
     *
     * Each component is added individually starting with years, months, ..., milliseconds
     */
    // TODO: test me
    fun minus(period: MpTemporalPeriod): MpZonedDateTime {
        return plus(-period)
    }
}

// TODO: test me
fun MpZonedDateTime.formatDdMmmYyyy(): String {
    return format("dd MMM yyyy")
}

// TODO: test me
fun MpZonedDateTime.formatHhMm(): String {
    return format("HH:mm")
}

// TODO: test me
fun MpZonedDateTime.formatDdMmmYyyyHhMm(): String {
    return format("dd MMM yyyy HH:mm")
}

// TODO: test me
fun MpZonedDateTime.formatDdMmmYyyyHhMmSs(): String {
    return format("dd MMM yyyy HH:mm:ss")
}
