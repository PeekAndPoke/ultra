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

/** Awaker for [MpLocalDate] values. Reads `ts` (epoch millis) and a required `timezone` zone id string. */
object MpLocalDateAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpLocalDate? {

        if (data !is Map<*, *>) {
            return null
        }

        return when (val ts = data[TS]) {
            // TODO(scan): unsafe cast - a missing or non-string "timezone" entry throws instead of
            //  awaking to null, unlike the kotlinx sibling (LocalDateCodec.kt), which defaults to UTC
            //  via a safe cast (`as? String`).
            is Number -> MpInstant.fromEpochMillis(ts.toLong())
                .atZone(TimeZone.of(data[TIMEZONE] as String))
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
            ts = data.atStartOfDay(TimeZone.UTC).toEpochMillis(),
            timezone = utc,
            human = data.toIsoString(),
        )
    }
}
