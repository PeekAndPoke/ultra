package de.peekandpoke.ultra.common.datetime

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

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpLocalDateSerializer::class)
data class MpLocalDate internal constructor(private val value: LocalDate) : Comparable<MpLocalDate> {

    companion object {
        fun of(year: Int, month: Int, day: Int): MpLocalDate = MpLocalDate(
            value = LocalDate(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
            )
        )

        fun of(year: Int, month: Month, day: Int): MpLocalDate = of(
            year = year,
            month = month.number,
            day = day,
        )

        fun parse(isoString: String): MpLocalDate {

            return try {
                MpLocalDate(
                    value = LocalDate.parse(isoString)
                )
            } catch (e: Throwable) {
                MpInstant.parse(isoString).atZone(TimeZone.UTC).toLocalDate()
            }
        }

        val Genesis: MpLocalDate = MpLocalDate(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP).toLocalDateTime(TimeZone.UTC).date
        )

        val Doomsday: MpLocalDate = MpLocalDate(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP).toLocalDateTime(TimeZone.UTC).date
        )
    }

    val year: Int get() = value.year
    val monthNumber: Int get() = value.monthNumber
    val month: Month get() = value.month
    val dayOfMonth: Int get() = value.dayOfMonth
    val dayOfWeek: DayOfWeek get() = value.dayOfWeek
    val dayOfYear: Int get() = value.dayOfYear

    override fun compareTo(other: MpLocalDate): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return "MpLocalDate(${toIsoString()})"
    }

    fun toIsoString(): String = atStartOfDay(TimeZone.UTC).toIsoString()

    fun atStartOfDay(timezone: TimeZone): MpInstant = MpInstant(
        value = value.atStartOfDayIn(timezone)
    )

    fun toLocalDateTime(): MpLocalDateTime = MpLocalDateTime(
        value = value.atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC)
    )

    fun plus(unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.plus(1, unit)
    )

    fun plus(amount: Int, unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.plus(amount, unit)
    )

    fun plus(amount: Long, unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.plus(amount, unit)
    )

    fun plus(period: DatePeriod): MpLocalDate = MpLocalDate(
        value = value.plus(period)
    )

    fun minus(unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.minus(1, unit)
    )

    fun minus(amount: Int, unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.minus(amount, unit)
    )

    fun minus(amount: Long, unit: DateTimeUnit.DateBased): MpLocalDate = MpLocalDate(
        value = value.minus(amount, unit)
    )

    fun minus(period: DatePeriod): MpLocalDate = MpLocalDate(
        value = value.plus(period)
    )
}


