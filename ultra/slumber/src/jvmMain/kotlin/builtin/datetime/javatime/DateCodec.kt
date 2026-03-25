package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import java.util.*

/** Awaker for [java.util.Date] values. */
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

/** Slumberer for [java.util.Date] values. */
object DateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is Date) {
            return null
        }

        return toMap(data.time, utc, data.toString())
    }
}
