package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.time.Instant

internal actual fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Double): Int {
    return TimeZone.of(zoneId).offsetAt(Instant.fromEpochSeconds(epochSeconds.toLong())).totalSeconds
}

internal actual fun platformSystemDefaultZoneId(): String {
    return TimeZone.currentSystemDefault().id
}
