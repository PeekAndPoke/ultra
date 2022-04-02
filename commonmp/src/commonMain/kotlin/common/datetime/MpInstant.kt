package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable(with = MpInstantSerializer::class)
data class MpInstant(val value: Instant) : Comparable<MpInstant> {

    companion object {
        fun fromEpochMillis(millis: Long): MpInstant = MpInstant(
            value = Instant.fromEpochMilliseconds(millis)
        )

        fun fromEpochSeconds(seconds: Long, nanosecondAdjustment: Number = 0): MpInstant = MpInstant(
            value = Instant.fromEpochSeconds(seconds, nanosecondAdjustment.toLong())
        )

        fun parse(isoString: String): MpInstant = MpInstant(
            value = Instant.parse(isoString)
        )

        val Genesis: MpInstant = MpInstant(
            Instant.fromEpochMilliseconds(GENESIS_TIMESTAMP)
        )

        val Doomsday: MpInstant = MpInstant(
            Instant.fromEpochMilliseconds(DOOMSDAY_TIMESTAMP)
        )
    }

    override fun compareTo(other: MpInstant): Int {
        return value.compareTo(other.value)
    }

    fun toIsoString(): String {
        return atZone(TimeZone.UTC).toIsoString()
    }

    fun toEpochMillis(): Long = value.toEpochMilliseconds()

    fun toEpochSeconds(): Long = value.epochSeconds

    fun plus(duration: Duration): MpInstant = MpInstant(
        value = value.plus(duration)
    )

    fun minus(duration: Duration): MpInstant = MpInstant(
        value = value.minus(duration)
    )

    fun atZone(timezone: TimeZone): MpZonedDateTime = MpZonedDateTime.of(
        value.toLocalDateTime(timezone),
        timezone,
    )
}
