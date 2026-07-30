package io.peekandpoke.ultra.slumber.builtin.datetime.mp

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import io.peekandpoke.ultra.slumber.builtin.datetime.TS
import io.peekandpoke.ultra.slumber.builtin.datetime.toMap
import kotlinx.datetime.TimeZone

/** Awaker for [MpZonedDateTime] values. Reads `ts` (epoch millis) and a required `timezone` zone id string. */
object MpZonedDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpZonedDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        // TODO(scan): unsafe cast, evaluated before the `ts` entry is even looked at - a map missing
        //  "timezone" (or holding a non-string value there) throws instead of awaking to null, no
        //  matter what "ts" contains. See MpLocalDateCodec.kt/MpLocalDateTimeCodec.kt for the same
        //  pattern, and kotlinx's LocalDateCodec.kt for the safe-cast-with-default alternative.
        val timezone = TimeZone.of(data[TIMEZONE] as String)

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
