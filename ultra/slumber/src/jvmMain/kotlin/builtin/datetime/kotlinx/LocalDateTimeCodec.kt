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

/**
 * Awaker for [LocalDateTime] values.
 *
 * Reads `ts` (epoch millis) and an optional `timezone` zone id string, defaulting to UTC when the
 * `timezone` entry is missing or not a string.
 */
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

/** Slumberer for [LocalDateTime] values. Writes `{ts: epoch millis (as if UTC), timezone: "Z", human}`. */
object LocalDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDateTime) {
            return null
        }

        val timezone = TimeZone.UTC
        val zoned = data.toInstant(timezone)

        // TODO(scan): TimeZone.UTC.id is "Z" (kotlinx-datetime's FixedOffsetTimeZone renders its id
        //  from the offset string), not "UTC" - every other UTC-writing slumberer in this module
        //  (including the sibling LocalDateCodec.kt, which reuses the shared java.time `utc` constant)
        //  writes the literal "UTC". Inconsistent wire value for the same concept.
        return toMap(
            zoned.toEpochMilliseconds(),
            TimeZone.UTC.id,
            zoned.toString(),
        )
    }
}
