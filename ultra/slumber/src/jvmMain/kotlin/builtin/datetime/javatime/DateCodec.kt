package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.util.Date

/** Awaker for [Date] values. Reads the `ts` map entry as epoch milliseconds; any `timezone` entry is ignored. */
object DateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Date? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> Date(ts.toLong())
            else -> null
        }
    }
}

/** Slumberer for [Date] values. Writes `{ts: epoch millis, timezone: "UTC", human}`. */
object DateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is Date) {
            return null
        }

        // TODO(scan): Date.toString() renders in the JVM default timezone, not UTC - the "human"
        //  field here is inconsistent with the "timezone": "UTC" field written alongside it, and
        //  with every sibling slumberer in this package (their `human` genuinely reflects UTC).
        return toMap(data.time, utc, data.toString())
    }
}
