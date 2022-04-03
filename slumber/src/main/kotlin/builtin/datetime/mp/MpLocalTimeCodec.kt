package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpLocalTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object MpLocalTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalTime? {

        if (data !is Number) {
            return null
        }

        return MpLocalTime(data.toLong())
    }
}

object MpLocalTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Long? {

        if (data !is MpLocalTime) {
            return null
        }

        return data.milliSeconds
    }
}
