package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Awaker for [java.time.ZonedDateTime] values. */
object ZonedDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): ZonedDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val ts = data[TS]
        val timezone = data[TIMEZONE]

        return when {
            ts is Number && timezone is String -> ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(ts.toLong()),
                ZoneId.of(timezone)
            )

            else -> null
        }
    }
}

/** Slumberer for [java.time.ZonedDateTime] values. */
object ZonedDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is ZonedDateTime) {
            return null
        }

        return toMap(data.toInstant().toEpochMilli(), data.zone, data.toString())
    }
}
