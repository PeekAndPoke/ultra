package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [MpTimezone] values. Reads any non-null [String] as a zone id. */
object MpTimezoneAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpTimezone? {

        if (data !is String) {
            return null
        }

        // TODO(scan): unlike the javatime sibling (ZoneIdCodec.kt, which whitelists against
        //  ZoneId.getAvailableZoneIds()), MpTimezone.of() performs no validation - an unknown zone id
        //  string awakes successfully here, and only fails later, at the first `.kotlinx` access.
        return MpTimezone.of(data)
    }
}

/** Slumberer for [MpTimezone] values. Writes the zone id as a plain [String], no wrapper map. */
object MpTimezoneSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is MpTimezone) {
            return null
        }

        return data.id
    }
}
