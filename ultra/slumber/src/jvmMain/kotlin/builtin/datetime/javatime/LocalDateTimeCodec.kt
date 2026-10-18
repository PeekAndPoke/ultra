package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.time.Instant
import java.time.LocalDateTime

/**
 * Awaker for [LocalDateTime] values.
 *
 * Reads the `ts` map entry as epoch milliseconds and always interprets it in UTC - any `timezone`
 * entry in the map is ignored.
 */
object LocalDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): LocalDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> LocalDateTime.ofInstant(Instant.ofEpochMilli(ts.toLong()), utc)
            else -> null
        }
    }
}

/** Slumberer for [LocalDateTime] values. Writes `{ts: epoch millis (as if UTC), timezone: "UTC", human}`. */
object LocalDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is LocalDateTime) {
            return null
        }

        val zoned = data.atZone(utc)

        return toMap(zoned.toInstant().toEpochMilli(), utc, zoned.toString())
    }
}
