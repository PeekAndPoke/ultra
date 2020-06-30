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

        if (data !is DateTime) {
            return null
        }

        return toMap(
            data.unixMillisLong,
            utc,
            data.toString(klockDateFormat)
        )
    }
}
