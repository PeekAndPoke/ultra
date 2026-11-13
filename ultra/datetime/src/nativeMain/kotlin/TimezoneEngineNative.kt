package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.time.Instant

// kotlinx/native re-reads and re-parses the zone file on every TimeZone.of(), and one conversion asks several
// times. Only ids in MpTimezone.supportedIds are cached; any other id that resolves gets only the memo slot below
// (ULTRA-00217).
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

private val zoneFileReadCount = AtomicInt(0)

/** How many times a zone file was read through [TimeZone.of], for tests. */
internal fun zoneFileReads(): Int = zoneFileReadCount.value

// One slot for the last unlisted id that resolved, keyed by the exact string: one conversion asks 3 to 5 times, a
// start of day inside a gap about 17 more. Replaced by the next unlisted id, so the caches stay bounded.
private val unlistedMemo = AtomicReference<Pair<String, TimeZone>?>(null)

/** The unlisted id in the one-slot memo, for tests. */
internal fun memoizedUnlistedZoneId(): String? = unlistedMemo.value?.first

private fun zoneFor(zoneId: String): TimeZone {
    zones.value[zoneId]?.let { return it }
    unlistedMemo.value?.let { (id, zone) -> if (id == zoneId) return zone }

    val zone = TimeZone.of(zoneId)
    zoneFileReadCount.incrementAndGet()

    if (zoneId !in MpTimezone.supportedIds) {
        unlistedMemo.value = zoneId to zone
        return zone
    }

    while (true) {
        val current = zones.value
        if (zones.compareAndSet(current, current + (zoneId to zone))) return zone
    }
}
