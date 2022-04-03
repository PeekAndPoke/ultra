package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpZonedDateTimeSerializer::class)
data class MpZonedDateTime private constructor(
    val datetime: MpLocalDateTime,
    val timezone: TimeZone
) : Comparable<MpZonedDateTime> {

    companion object {
        fun of(value: MpLocalDateTime, timezone: TimeZone): MpZonedDateTime {
            return MpZonedDateTime(
                datetime = value,
                timezone = when (timezone.id) {
                    // WHY? There seems to be a difference between TimeZone.UTC and TimeZone.of("UTC")
                    "UTC" -> TimeZone.UTC
                    else -> timezone
                }
            )
        }

        fun parse(isoString: String, timezone: TimeZone): MpZonedDateTime {
            return MpZonedDateTime(
                datetime = MpDateTimeParser.parseInstant(isoString).atZone(TimeZone.UTC).datetime,
                timezone = timezone
            )
        }

        fun parse(isoString: String): MpZonedDateTime = MpDateTimeParser.parseZonedDateTime(isoString)
    }

    val year: Int get() = datetime.year
    val monthNumber: Int get() = datetime.monthNumber
    val month: Month get() = datetime.month
    val dayOfMonth: Int get() = datetime.dayOfMonth
    val dayOfWeek: DayOfWeek get() = datetime.dayOfWeek
    val dayOfYear: Int get() = datetime.dayOfYear

    val hour: Int get() = datetime.hour
    val minute: Int get() = datetime.minute
    val second: Int get() = datetime.second
    val nanosecond: Int get() = datetime.nanosecond

    private val instant: MpInstant = datetime.toInstant(timezone)

    override fun compareTo(other: MpZonedDateTime): Int {
        return instant.compareTo(other.instant)
    }

    override fun toString(): String {
        return "MpZonedDateTime(${toIsoString()})"
    }

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

    fun toInstant(): MpInstant = instant

    fun toLocalDate(): MpLocalDate = datetime.toDate()

    fun toLocalTime(): MpLocalTime = datetime.toTime()

    fun toLocalDateTime(): MpLocalDateTime = datetime

    fun toEpochMillis(): Long = toInstant().toEpochMillis()

    fun toEpochSeconds(): Long = toInstant().toEpochSeconds()
}
