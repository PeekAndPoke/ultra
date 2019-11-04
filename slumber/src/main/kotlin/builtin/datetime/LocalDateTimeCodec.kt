package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant
import java.time.LocalDateTime

object LocalDateTimeCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): LocalDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> LocalDateTime.ofInstant(Instant.ofEpochMilli(ts.toLong()), utc)
            else -> null
        }
    }

    override fun slumber(data: Any?): Map<String, Any>? {

        if (data !is LocalDateTime) {
            return null
        }

        val zoned = data.atZone(utc)

        return toMap(zoned.toInstant().toEpochMilli(), utc, zoned.toString())
    }
}
