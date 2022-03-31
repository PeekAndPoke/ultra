package de.peekandpoke.ultra.slumber.builtin.datetime.portable

import de.peekandpoke.ultra.common.datetime.PortableTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object PortableTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): PortableTime? {

        if (data !is Number) {
            return null
        }

        return PortableTime(data.toLong())
    }
}

object PortableTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Long? {

        if (data !is PortableTime) {
            return null
        }

        return data.milliSeconds
    }
}
