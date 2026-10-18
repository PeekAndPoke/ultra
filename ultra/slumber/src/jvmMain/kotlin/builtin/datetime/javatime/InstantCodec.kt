package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.time.Instant

/** Awaker for [Instant] values. Reads the `ts` map entry as epoch milliseconds; timezone-free by nature. */
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

/** Slumberer for [Instant] values. Writes `{ts: epoch millis, timezone: "UTC", human}`. */
object InstantSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is Instant) {
            return null
        }

        return toMap(data.toEpochMilli(), utc, data.toString())
    }
}
