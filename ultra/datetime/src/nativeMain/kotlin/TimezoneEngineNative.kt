package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.concurrent.AtomicReference
import kotlin.time.Instant

// kotlinx/native re-reads and re-parses the zone file on every TimeZone.of(), and one conversion asks several
// times. Only ids in MpTimezone.supportedIds are cached: kotlinx/native also resolves path variants of an id
// ("Europe/./Berlin", "Europe//Berlin"), which would each add an entry (ULTRA-00217). Other ids resolve uncached.
private val zones = AtomicReference<Map<String, TimeZone>>(emptyMap())

internal actual fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Double): Int {
    return zoneFor(zoneId).offsetAt(Instant.fromEpochSeconds(epochSeconds.toLong())).totalSeconds
}

internal actual fun platformOffsetSecondsAtExact(zoneId: String, epochSeconds: Long): Int {
    return zoneFor(zoneId).offsetAt(Instant.fromEpochSeconds(epochSeconds)).totalSeconds
}

internal actual fun platformSystemDefaultZoneId(): String {
    return TimeZone.currentSystemDefault().id
}

/** The ids in the zone cache, for tests. */
internal fun cachedZoneIds(): Set<String> = zones.value.keys

private fun zoneFor(zoneId: String): TimeZone {
    zones.value[zoneId]?.let { return it }

    val zone = TimeZone.of(zoneId)

    if (zoneId !in MpTimezone.supportedIds) return zone

    while (true) {
        val current = zones.value
        if (zones.compareAndSet(current, current + (zoneId to zone))) return zone
    }
}
