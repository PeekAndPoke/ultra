package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object ZonedDateTimeCodec : Awaker, Slumberer {

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

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is ZonedDateTime) {
            return null
        }

        return toMap(data.toInstant().toEpochMilli(), data.zone, data.toString())
    }
}
