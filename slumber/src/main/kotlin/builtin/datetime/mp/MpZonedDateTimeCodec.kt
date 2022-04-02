package de.peekandpoke.ultra.slumber.builtin.datetime.mp

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.datetime.TIMEZONE
import de.peekandpoke.ultra.slumber.builtin.datetime.TS
import de.peekandpoke.ultra.slumber.builtin.datetime.toMap
import kotlinx.datetime.TimeZone

object MpZonedDateTimeAwaker : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): MpZonedDateTime? {

        if (data !is Map<*, *>) {
            return null
        }

        val timezone = TimeZone.of(data[TIMEZONE] as String)

        return when (val ts = data[TS]) {
            is Number -> MpZonedDateTime.of(
                value = MpInstant.fromEpochMillis(ts.toLong()).atZone(timezone).datetime,
                timezone = timezone,
            )

            else -> null
        }
    }
}

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
