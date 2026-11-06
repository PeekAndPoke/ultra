package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.mpTimezoneOrNull
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import io.peekandpoke.ultra.slumber.builtin.datetime.utc

/** Awaker for [MpLocalDate] values. Reads `ts` (epoch millis) and a required `timezone` zone id string; null without both. */
object MpLocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = mpTimezoneOrNull(data[TIMEZONE]) ?: return null

        return when (val ts = data[TS]) {
            is Number -> MpInstant.fromEpochMillis(ts.toLong())
                .atZone(timezone)
                .toLocalDate()

            else -> null
        }
    }
}

/** Slumberer for [MpLocalDate] values. Writes `{ts: epoch millis at start-of-day UTC, timezone: "UTC", human}`. */
object MpLocalDateSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpLocalDate) {
            return null
        }

        return toMap(
            ts = data.atStartOfDay(MpTimezone.UTC).toEpochMillis(),
            timezone = utc,
            human = data.toIsoString(),
        )
    }
}
