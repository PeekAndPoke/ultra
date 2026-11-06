package io.peekandpoke.ultra.slumber.builtin.datetime.kotlinx

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.kotlinxZoneOrNull
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Awaker for [LocalDate] values.
 *
 * Reads `ts` (epoch millis) and an optional `timezone` zone id string, defaulting to UTC when the
 * `timezone` entry is missing or not a string; null for a zone no one knows.
 */
object LocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): LocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = when (val id = data[TIMEZONE]) {
            is String -> kotlinxZoneOrNull(id) ?: return null
            else -> TimeZone.UTC
        }

        return when (val ts = data[TS]) {
            is Number -> {
                val instant = Instant.fromEpochMilliseconds(ts.toLong())

                instant.toLocalDateTime(timezone).date
            }
            else -> null
        }
    }
}

/** Slumberer for [LocalDate] values. Writes `{ts: epoch millis at start-of-day UTC, timezone: "UTC", human}`. */
object LocalDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDate) {
            return null
        }

        val zoned = data.atStartOfDayIn(TimeZone.UTC)

        return toMap(
            zoned.toEpochMilliseconds(),
            utc,
            zoned.toString()
        )
    }
}
