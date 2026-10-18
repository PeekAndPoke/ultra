package io.peekandpoke.ultra.slumber.builtin.datetime.javatime

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import java.time.ZoneId

/**
 * Awaker for [ZoneId] values.
 *
 * Reads a zone id [String], validated against [ZoneId.getAvailableZoneIds]. Note that set excludes
 * offset-style ids such as `"Z"` or `"+02:00"`, even though [ZoneId.of] itself accepts them.
 */
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

/** Slumberer for [ZoneId] values. Writes the zone id as a plain [String], no wrapper map. */
object ZoneIdSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is ZoneId) {
            return null
        }

        return data.id
    }
}
