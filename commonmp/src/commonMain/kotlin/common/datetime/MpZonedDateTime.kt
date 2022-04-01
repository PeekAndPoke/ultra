package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable

@Suppress("DataClassPrivateConstructor")
@Serializable(with = MpZonedDateTimeSerializer::class)
data class MpZonedDateTime private constructor(
    val value: LocalDateTime,
    val timezone: TimeZone
) : Comparable<MpZonedDateTime> {

    companion object {
        private val isoWithTimezone = "(.*)\\[(.*)]".toRegex()

        fun of(value: LocalDateTime, timezone: TimeZone): MpZonedDateTime {

            return MpZonedDateTime(
                value = value,
                timezone = when (timezone.id) {
                    // WHY? There seems to be a difference between TimeZone.UTC and TimeZone.of("UTC")
                    "UTC" -> TimeZone.UTC
                    else -> timezone
                }
            )

        }

        fun parse(isoString: String, timezone: TimeZone): MpZonedDateTime = of(
            value = LocalDateTime.parse(isoString),
            timezone = timezone
        )

        fun parse(isoString: String): MpZonedDateTime {

            val regexMatch by lazy {
                isoWithTimezone.find(isoString)
            }

            return when {
                isoString.last() == 'Z' -> parse(isoString.dropLast(1), TimeZone.UTC)

                regexMatch != null -> {
                    val groupsValues = regexMatch!!.groupValues

                    parse(groupsValues[1], TimeZone.of(groupsValues[2]))
                }

                else -> parse(isoString, TimeZone.UTC)
            }
        }
    }

    private val instant: MpInstant = MpInstant(
        value = value.toInstant(timezone)
    )

    override fun compareTo(other: MpZonedDateTime): Int {
        return instant.compareTo(other.instant)
    }

    override fun toString(): String {
        return when (timezone) {
            TimeZone.UTC, TimeZone.of("UTC") -> "${value}Z"
            else -> "${value}[$timezone]"
        }
    }

    fun toInstant(): MpInstant = instant

    fun toEpochMillis(): Long = toInstant().toEpochMillis()

    fun toEpochSeconds(): Long = toInstant().toEpochSeconds()
}
