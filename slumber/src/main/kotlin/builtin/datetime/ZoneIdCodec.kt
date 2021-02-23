package de.peekandpoke.ultra.slumber.builtin.datetime

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import java.time.ZoneId

object ZoneIdAwaker : Awaker {

    private val ids = ZoneId.getAvailableZoneIds()

    override fun awake(data: Any?, context: Awaker.Context): ZoneId? {

        if (data !is String) {
            return null
        }

        return when (data) {
            in ids -> ZoneId.of(data)
            else -> null
        }
    }
}

object ZoneIdSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is ZoneId) {
            return null
        }

        return data.id
    }
}
