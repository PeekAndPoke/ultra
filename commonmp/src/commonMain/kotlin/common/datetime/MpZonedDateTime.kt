package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpZonedDateTimeSerializer::class)
data class MpZonedDateTime private constructor(
    val datetime: MpLocalDateTime,
    val timezone: TimeZone
) : Comparable<MpZonedDateTime> {

    companion object {
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

        fun parse(isoString: String, timezone: TimeZone): MpZonedDateTime {
            return MpZonedDateTime(
                datetime = MpDateTimeParser.parseInstant(isoString).atZone(TimeZone.UTC).datetime,
                timezone = timezone
            )
        }

        fun parse(isoString: String): MpZonedDateTime = MpDateTimeParser.parseZonedDateTime(isoString)

        // TODO: test me
        val Genesis: MpZonedDateTime = MpInstant.Genesis.atZone(TimeZone.UTC)

        // TODO: test me
        val Doomsday: MpZonedDateTime = MpInstant.Doomsday.atZone(TimeZone.UTC)
    }

    private val instant: MpInstant = datetime.toInstant(timezone)

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

    // TODO: test me
    val atStartOfDay
        get() = copy(
            datetime = MpLocalDateTime.of(year, month, dayOfMonth)
        )

    // TODO: test me
    val atStartOfHour
        get() = copy(
            datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour)
        )

    // TODO: test me
    val atStartOfMinute
        get() = copy(
            datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour, minute)
        )

    // TODO: test me
    val atStartOfSecond
        get() = copy(
            datetime = MpLocalDateTime.of(year, month, dayOfMonth, hour, minute, second)
        )

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

    // TODO: test me
    fun plus(duration: Duration) = toInstant().plus(duration).atZone(timezone)

    // TODO: test me
    fun minus(duration: Duration) = toInstant().minus(duration).atZone(timezone)
}
