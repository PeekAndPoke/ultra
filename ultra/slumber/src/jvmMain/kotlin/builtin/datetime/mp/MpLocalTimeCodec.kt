package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [MpLocalTime] values. */
object MpLocalTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalTime? {

        if (data !is Number) {
            return null
        }

        return MpLocalTime.ofMilliSeconds(data.toLong())
    }
}

/** Slumberer for [MpLocalTime] values. */
object MpLocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Long? {

        if (data !is MpLocalTime) {
            return null
        }

        return data.inWholeMilliSeconds()
    }
}
