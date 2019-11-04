package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant
import java.time.LocalDateTime

object LocalDateTimeCodec : Awaker, Slumberer {

    override fun awake(data: Any?): LocalDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val ts = data[TS]

        if (ts !is Number) {
            return null
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts.toLong()), utc)
    }

    override fun slumber(data: Any?): Map<String, Any>? {

        if (data !is LocalDateTime) {
            return null
        }

        val zoned = data.atZone(utc)

        return toMap(zoned.toInstant().toEpochMilli(), utc, zoned.toString())
    }
}
