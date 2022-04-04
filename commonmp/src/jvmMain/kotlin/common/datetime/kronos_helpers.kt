package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone
import java.time.ZoneId

fun Kronos.zonedDateTimeNow(timezone: ZoneId): MpZonedDateTime {
    return zonedDateTimeNow(TimeZone.of(timezone.id))
}
