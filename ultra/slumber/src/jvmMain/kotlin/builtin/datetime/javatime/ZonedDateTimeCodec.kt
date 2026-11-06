package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.javaZoneOrNull
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import java.time.Instant
import java.time.ZonedDateTime

/** Awaker for [ZonedDateTime] values. Reads `{ts: epoch millis, timezone: zone id string}`. */
object ZonedDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): ZonedDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val ts = data[TS]
        val timezone = javaZoneOrNull(data[TIMEZONE])

        return when {
            ts is Number && timezone != null -> ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(ts.toLong()),
                timezone,
            )

            else -> null
        }
    }
}

/** Slumberer for [ZonedDateTime] values. Writes `{ts: epoch millis, timezone: zone id string, human}`. */
object ZonedDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is ZonedDateTime) {
            return null
        }

        return toMap(data.toInstant().toEpochMilli(), data.zone, data.toString())
    }
}
