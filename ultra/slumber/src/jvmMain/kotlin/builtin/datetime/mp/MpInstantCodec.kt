package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc

object MpInstantAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpInstant? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            is Number -> MpInstant.fromEpochMillis(ts.toLong())
            else -> null
        }
    }
}

object MpInstantSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpInstant) {
            return null
        }

        return toMap(
            ts = data.toEpochMillis(),
            timezone = utc,
            human = data.toIsoString(),
        )
    }
}
