package de.peekandpoke.ultra.slumber.builtin.datetime.portable

import de.peekandpoke.ultra.common.datetime.PortableTimezone
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object PortableTimezoneAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): PortableTimezone? {

        if (data !is String) {
            return null
        }

        return PortableTimezone(data)
    }
}

object PortableTimezoneSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is PortableTimezone) {
            return null
        }

        return data.id
    }
}
