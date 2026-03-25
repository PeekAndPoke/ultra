package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc
import kotlinx.datetime.TimeZone

object MpLocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> MpInstant.fromEpochMillis(ts.toLong())
                .atZone(TimeZone.of(data[TIMEZONE] as String))
                .toLocalDate()

            else -> null
        }
    }
}

object MpLocalDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpLocalDate) {
            return null
        }

        return toMap(
            ts = data.atStartOfDay(TimeZone.UTC).toEpochMillis(),
            timezone = utc,
            human = data.toIsoString(),
        )
    }
}
