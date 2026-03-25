package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

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
