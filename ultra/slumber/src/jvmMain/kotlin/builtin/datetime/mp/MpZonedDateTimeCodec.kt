package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.mpTimezoneOrNull
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap

/** Awaker for [MpZonedDateTime] values. Reads `ts` (epoch millis) and a required `timezone` zone id string; null without both. */
object MpZonedDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpZonedDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = mpTimezoneOrNull(data[TIMEZONE]) ?: return null

        return when (val ts = data[TS]) {
            is Number -> MpZonedDateTime.of(
                datetime = MpInstant.fromEpochMillis(ts.toLong()).atZone(timezone).datetime,
                timezone = timezone,
            )

            else -> null
        }
    }
}

/** Slumberer for [MpZonedDateTime] values. Writes `{ts: epoch millis, timezone: zone id string, human: isoString}`. */
object MpZonedDateTimeSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any>? {

        if (data !is MpZonedDateTime) {
            return null
        }

        return toMap(
            ts = data.toInstant().toEpochMillis(),
            timezone = data.timezone.id,
            human = data.toIsoString(),
        )
    }
}
