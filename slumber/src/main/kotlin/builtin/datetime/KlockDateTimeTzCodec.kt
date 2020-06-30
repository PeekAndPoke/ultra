package de.peekandpoke.ultra.slumber.builtin.datetime

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeTz
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object KlockDateTimeTzAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): DateTimeTz? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> DateTime.fromUnix(ts.toDouble()).utc
            else -> null
        }
    }
}

object KlockDateTimeTzSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is DateTimeTz) {
            return null
        }

        return toMap(
            data.utc.unixMillisLong,
            utc,
            data.toString(klockDateFormat)
        )
    }
}
