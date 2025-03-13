package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object MpTimezoneAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpTimezone? {

        if (data !is String) {
            return null
        }

        return MpTimezone.of(data)
    }
}

object MpTimezoneSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is MpTimezone) {
            return null
        }

        return data.id
    }
}
