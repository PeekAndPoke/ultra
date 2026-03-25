package io.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Awaker for [kotlinx.datetime.LocalDateTime] values. */
object LocalDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): LocalDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = (data[TIMEZONE] as? String?)?.let { tz -> TimeZone.of(tz) } ?: TimeZone.UTC

        return when (val ts = data[TS]) {
            is Number -> {
                val instant = Instant.fromEpochMilliseconds(ts.toLong())

                instant.toLocalDateTime(timezone)
            }

            else -> null
        }
    }
}

/** Slumberer for [kotlinx.datetime.LocalDateTime] values. */
object LocalDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDateTime) {
            return null
        }

        val timezone = TimeZone.UTC
        val zoned = data.toInstant(timezone)

        return toMap(
            zoned.toEpochMilliseconds(),
            TimeZone.UTC.id,
            zoned.toString(),
        )
    }
}
