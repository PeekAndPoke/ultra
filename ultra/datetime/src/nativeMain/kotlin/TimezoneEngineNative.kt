package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.concurrent.AtomicReference
import kotlin.time.Instant

// kotlinx/native re-reads and re-parses the zone file on every TimeZone.of(), and one conversion asks several
// times. Only ids that resolved are cached, so the map is bounded by the system's tz database.
private val zones = AtomicReference<Map<String, TimeZone>>(emptyMap())

internal actual fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Double): Int {
    return zoneFor(zoneId).offsetAt(Instant.fromEpochSeconds(epochSeconds.toLong())).totalSeconds
}

internal actual fun platformSystemDefaultZoneId(): String {
    return TimeZone.currentSystemDefault().id
}

private fun zoneFor(zoneId: String): TimeZone {
    zones.value[zoneId]?.let { return it }

    val zone = TimeZone.of(zoneId)

    while (true) {
        val current = zones.value
        if (zones.compareAndSet(current, current + (zoneId to zone))) return zone
    }
}
