package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.DateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object KlockDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): DateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> DateTime.fromUnix(ts.toDouble())
            else -> null
        }
    }
}

object KlockDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        // We need to check for a pure Double value as DateTime is an inline class
        val use = when (data) {
            is Number -> DateTime(data.toDouble())
            is DateTime -> data
            else -> return null
        }

        return toMap(
            use.unixMillisLong,
            utc,
            use.toString(klockDateFormat)
        )
    }
}
