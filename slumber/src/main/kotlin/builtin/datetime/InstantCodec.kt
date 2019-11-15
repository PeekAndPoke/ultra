package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.Instant

object InstantAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Instant? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> Instant.ofEpochMilli(ts.toLong())
            else -> null
        }
    }
}

object InstantSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is Instant) {
            return null
        }

        return toMap(data.toEpochMilli(), utc, data.toString())
    }
}
