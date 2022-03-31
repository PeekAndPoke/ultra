package de.peekandpoke.ultra.slumber.builtin.datetime.javatime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import de.peekandpoke.ultra.slumber.builtin.datetime.TS
import de.peekandpoke.ultra.slumber.builtin.datetime.toMap
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

object ZonedDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is ZonedDateTime) {
            return null
        }

        return toMap(data.toInstant().toEpochMilli(), data.zone, data.toString())
    }
}
