package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import de.peekandpoke.ultra.slumber.builtin.datetime.TS
import de.peekandpoke.ultra.slumber.builtin.datetime.toMap
import de.peekandpoke.ultra.slumber.builtin.datetime.utc
import kotlinx.datetime.TimeZone

object MpLocalDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> MpInstant.fromEpochMillis(ts.toLong())
                .atZone(TimeZone.of(data[TIMEZONE] as String))
                .datetime

            else -> null
        }
    }
}

object MpLocalDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpLocalDateTime) {
            return null
        }

        return toMap(
            ts = data.toInstant(TimeZone.UTC).toEpochMillis(),
            timezone = utc,
            human = data.toIsoString(),
        )
    }
}
