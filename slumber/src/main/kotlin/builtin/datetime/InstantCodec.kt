package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant

object InstantCodec : Awaker, Slumberer {

    override fun awake(data: Any?): Instant? {

        if (data !is Map<*, *>) {
            return null
        }

        val ts = data[TS]

        if (ts !is Number) {
            return null
        }

        return Instant.ofEpochMilli(ts.toLong())
    }

    override fun slumber(data: Any?): Map<String, Any>? {

        if (data !is Instant) {
            return null
        }

        return toMap(data.toEpochMilli(), utc, data.toString())
    }
}
