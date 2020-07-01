package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.Date
import com.soywiz.klock.DateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object KlockDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Date? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> DateTime.fromUnix(ts.toDouble()).date
            else -> null
        }
    }
}

object KlockDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is Date) {
            return null
        }

        val dateTime = data.dateTimeDayStart

        return toMap(
            dateTime.unixMillisLong,
            utc,
            dateTime.toString(klockDateFormat)
        )
    }
}
