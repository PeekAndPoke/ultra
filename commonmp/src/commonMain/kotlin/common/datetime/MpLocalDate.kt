package de.peekandpoke.ultra.common.datetime

import korlibs.time.Date
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Suppress("Detekt:TooManyFunctions")
@Serializable(with = MpLocalDateSerializer::class)
data class MpLocalDate internal constructor(
    private val value: LocalDate,
) : Comparable<MpLocalDate> {

    companion object {
        /**
         * The Genesis, a date in the distant past: -10000-01-01T00:00:00Z
         */
        val Genesis: MpLocalDate = MpLocalDate(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP).toLocalDateTime(TimeZone.UTC).date
        )

        /**
         * The Doomsday, a date in the distant future: +10000-01-01T00:00:00Z
         */
        val Doomsday: MpLocalDate = MpLocalDate(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP).toLocalDateTime(TimeZone.UTC).date
        )

        /**
         * Creates an [MpLocalDate] from [year], [month] and [day].
         */
        fun of(year: Int, month: Int, day: Int): MpLocalDate = MpLocalDate(
            value = LocalDate(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
            )
        )

        /**
         * Creates an [MpLocalDate] from [year], [month] and [day].
         */
        fun of(year: Int, month: Month, day: Int): MpLocalDate = of(
            year = year,
            month = month.number,
            day = day,
        )

        /**
         * Parses an [MpLocalDate] from the given [isoString].
         *
         * Throws an [IllegalArgumentException] if parsing fails.
         */
        @Throws(IllegalArgumentException::class)
        fun parse(isoString: String): MpLocalDate {
            return try {
                parseInternal(isoString)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse MpLocalDate from '$isoString'", e)
            }
        }

        /**
         * Parses an [MpLocalDate] from the given [isoString].
         *
         * Returns null if parsing fails.
         */
        fun tryParse(isoString: String): MpLocalDate? {
            return try {
                parseInternal(isoString)
            } catch (e: Throwable) {
                null
            }
        }

        private fun parseInternal(isoString: String): MpLocalDate {
            return try {
                MpLocalDate(LocalDate.parse(isoString))
            } catch (e: Throwable) {
                MpInstant.parse(isoString).atZone(TimeZone.UTC).toLocalDate()
            }
        }
    }

    /** The year */
    val year: Int get() = value.year

    /** The month as number, where January is "1" */
    val monthNumber: Int get() = value.monthNumber

    /** The month */
    val month: Month get() = value.month

    /** The day of the month */
    val day: Int get() = value.dayOfMonth

    /** The day of the week */
    val dayOfWeek: DayOfWeek get() = value.dayOfWeek

    /** The day of the year */
    val dayOfYear: Int get() = value.dayOfYear

    /**
     * Compares to the [other].
     */
    override fun compareTo(other: MpLocalDate): Int {
        return value.compareTo(other.value)
    }

    /**
     * Converts into a string.
     */
    override fun toString(): String {
        return "MpLocalDate(${toIsoString()})"
    }

    /**
     * Converts into a human-readable string by the given [formatString].
     *
     * See https://help.gooddata.com/cloudconnect/manual/date-and-time-format.html
     */
    fun format(formatString: String): String {
        val klock = Date(year = year, month = monthNumber, day = day)

        return klock.format(formatString)
    }

    /**
     * Converts into an iso string.
     */
    fun toIsoString(): String {
        return atStartOfDay(TimeZone.UTC).toIsoString()
    }

    /**
     * Gets the start of the century that this date is in.
     */
    fun atStartOfCentury(): MpLocalDate {
        return of(year = (year / 100) * 100, month = Month.JANUARY, day = 1)
    }

    /**
     * Gets the start of the century that this date is in.
     */
    fun atStartOfDecade(): MpLocalDate {
        return of(year = (year / 10) * 10, month = Month.JANUARY, day = 1)
    }

    /**
     * Gets the start of the year that this date is in.
     */
    fun atStartOfYear(): MpLocalDate {
        return of(year = year, month = Month.JANUARY, day = 1)
    }

    /**
     * Gets the start of the half of the year that this date is in.
     */
    fun atStartOfHalfOfYear(): MpLocalDate {
        return of(year = year, month = 1 + ((monthNumber - 1) / 6) * 6, day = 1)
    }

    /**
     * Gets the start of the quarter of the year that this date is in.
     */
    fun atStartOfQuarterOfYear(): MpLocalDate {
        return of(year = year, month = 1 + ((monthNumber - 1) / 3) * 3, day = 1)
    }

    /**
     * Gets the start of the month that this date is in.
     */
    fun atStartOfMonth(): MpLocalDate {
        return of(year = year, month = month, day = 1)
    }

    /**
     * Gets the upcoming day that is of the given [dayOfWeek].
     */
    fun atStartOfNext(dayOfWeek: DayOfWeek): MpLocalDate {

        var result = plus(1, DateTimeUnit.DAY)

        while (result.dayOfWeek != dayOfWeek) {
            result = result.plus(1, DateTimeUnit.DAY)
        }

        return result
    }

    /**
     * Gets the previous day that is of the given [dayOfWeek].
     */
    fun atStartOfPrevious(dayOfWeek: DayOfWeek): MpLocalDate {

        var result = this

        while (result.dayOfWeek != dayOfWeek) {
            result = result.minus(1, DateTimeUnit.DAY)
        }

        return result
    }

    /**
     * Converts into an [MpZonedDateTime] at the given [timezone].
     */
    fun atStartOfDay(timezone: MpTimezone): MpZonedDateTime {
        return atStartOfDay(timezone.kotlinx)
    }

    /**
     * Converts into an [MpZonedDateTime] at the given [timezone].
     */
    fun atStartOfDay(timezone: TimeZone): MpZonedDateTime {
        return MpInstant(value.atStartOfDayIn(timezone)).atZone(timezone)
    }

    /**
     * Converts into an [MpLocalDateTime] by setting the given [time].
     */
    fun atTime(time: MpLocalTime): MpLocalDateTime {
        return MpLocalDateTime.of(date = this, time = time)
    }

    /**
     * Converts into an [MpZonedDateTime] in the given [timezone] by setting the given [time].
     */
    fun atTime(time: MpLocalTime, timezone: TimeZone): MpZonedDateTime {
        return atStartOfDay(timezone).atTime(time)
    }

    /**
     * Converts into an [MpZonedDateTime] in the given [timezone] by setting the given [time].
     */
    fun atTime(time: MpLocalTime, timezone: MpTimezone): MpZonedDateTime {
        return atTime(time, timezone.kotlinx)
    }

    /**
     * Converts into an [MpLocalDateTime] at the start of the day.
     */
    fun toLocalDateTime(): MpLocalDateTime {
        return atTime(MpLocalTime.Min)
    }

    /**
     * Creates a [MpLocalDateRange] with this as the start and the given [period]
     */
    fun toRange(period: MpDatePeriod): MpLocalDateRange {
        return MpLocalDateRange(from = this, to = this.plus(period))
    }

    /**
     * Creates a [MpClosedLocalDateRange] with this as the start and the given [period]
     */
    fun toClosedRange(period: MpDatePeriod): MpClosedLocalDateRange {
        return MpClosedLocalDateRange(from = this, to = this.plus(period))
    }

    /**
     * Creates a [MpZonedDateTimeRange] with this as the start and the given [period] in the [timezone].
     */
    // TODO: test me
    fun toRange(period: MpTemporalPeriod, timezone: MpTimezone): MpZonedDateTimeRange {
        return MpZonedDateTimeRange.of(from = atStartOfDay(timezone), period = period)
    }

    /**
     * Creates an [MpLocalDateRange] from this to [end].
     */
    fun toRange(end: MpLocalDate): MpLocalDateRange {
        return MpLocalDateRange(this, end)
    }

    /**
     * Creates an [MpClosedLocalDateRange] from this to [end].
     */
    fun toClosedRange(end: MpLocalDate): MpClosedLocalDateRange {
        return MpClosedLocalDateRange(from = this, to = end)
    }

    /**
     * Converts into an [MpZonedDateTimeRange] for the given [timeslot] and [timezone].
     */
    fun toRange(timeslot: MpLocalTimeSlot, timezone: TimeZone): MpZonedDateTimeRange {
        return MpZonedDateTimeRange(
            from = atTime(timeslot.from, timezone),
            to = atTime(timeslot.to, timezone)
        )
    }

    /**
     * Converts into an [MpZonedDateTimeRange] for the given [timeslot] and [timezone].
     */
    fun toRange(timeslot: MpLocalTimeSlot, timezone: MpTimezone): MpZonedDateTimeRange {
        return toRange(timeslot, timezone.kotlinx)
    }

    /**
     * Adds the given [unit] once.
     */
    fun plus(unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.plus(1, unit)
        )
    }

    /**
     * Subtracts the given [unit] once.
     */
    fun minus(unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.minus(1, unit)
        )
    }

    /**
     * Adds [amount] times the given [unit].
     */
    fun plus(amount: Int, unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.plus(amount, unit)
        )
    }

    /**
     * Subtracts [amount] times the given [unit].
     */
    fun minus(amount: Int, unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.minus(amount, unit)
        )
    }

    /**
     * Adds [amount] times the given [unit].
     */
    fun plus(amount: Long, unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.plus(amount, unit)
        )
    }

    /**
     * Subtracts [amount] times the given [unit].
     */
    fun minus(amount: Long, unit: DateTimeUnit.DateBased): MpLocalDate {
        return MpLocalDate(
            value = value.minus(amount, unit)
        )
    }

    /**
     * Adds the given [DatePeriod].
     */
    // TODO: test me
    fun plus(period: MpTemporalPeriod): MpLocalDate {
        return plusYears(period.years)
            .plusMonths(period.months)
            .plusDays(period.days)
    }

    /**
     * Adds the given [DatePeriod].
     */
    // TODO: test me
    fun minus(period: MpTemporalPeriod): MpLocalDate {
        return plus(-period)
    }

    /**
     * Adds the given number of [days].
     */
    fun plusDays(days: Int): MpLocalDate {
        return plus(days, DateTimeUnit.DAY)
    }

    /**
     * Adds the given number of [days].
     */
    fun minusDays(days: Int): MpLocalDate {
        return plusDays(-days)
    }

    /**
     * Adds the given number of [weeks].
     */
    fun plusWeeks(weeks: Int): MpLocalDate {
        return plus(weeks, DateTimeUnit.WEEK)
    }

    /**
     * Adds the given number of [weeks].
     */
    fun minusWeeks(weeks: Int): MpLocalDate {
        return plusWeeks(-weeks)
    }

    /**
     * Adds the given number of [months].
     */
    fun plusMonths(months: Int): MpLocalDate {
        return plus(months, DateTimeUnit.MONTH)
    }

    /**
     * Adds the given number of [months].
     */
    fun minusMonths(months: Int): MpLocalDate {
        return plusMonths(-months)
    }

    /**
     * Adds the given number of [years].
     */
    fun plusYears(years: Int): MpLocalDate {
        return plus(years, DateTimeUnit.YEAR)
    }

    /**
     * Adds the given number of [years].
     */
    fun minusYears(years: Int): MpLocalDate {
        return plusYears(-years)
    }

    /**
     * Adds the given number of [centuries].
     */
    fun plusCenturies(centuries: Int): MpLocalDate {
        return plus(centuries, DateTimeUnit.CENTURY)
    }

    /**
     * Adds the given number of [centuries].
     */
    fun minusCenturies(centuries: Int): MpLocalDate {
        return plusCenturies(-centuries)
    }
}

/**
 * Formats the date as 'dd MMM yyyy'
 */
fun MpLocalDate.formatDdMmmYyyy(): String {
    return format("dd MMM yyyy")
}
