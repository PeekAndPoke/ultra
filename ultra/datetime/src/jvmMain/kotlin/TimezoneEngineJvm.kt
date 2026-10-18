package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.time.Instant

internal actual fun platformOffsetSecondsAt(zoneId: String, instant: Instant): Int {
    return TimeZone.of(zoneId).offsetAt(instant).totalSeconds
}

internal actual fun platformSystemDefaultZoneId(): String {
    return TimeZone.currentSystemDefault().id
}
