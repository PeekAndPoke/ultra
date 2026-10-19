@file:Suppress("UnsafeCastFromDynamic")

package io.peekandpoke.ultra.datetime

import kotlinx.datetime.IllegalTimeZoneException
import kotlin.js.Date
import kotlin.math.round

// Zone offsets come from the browser's own IANA database via Intl.DateTimeFormat — no tz data is bundled.

private val intl: dynamic = js("Intl")
// No prototype: ids like "constructor" must miss the cache, not find an Object.prototype member.
private val formatterCache: dynamic = js("Object.create(null)")

// JS Date covers ±8.64e15 ms; Intl throws a RangeError outside of it.
private const val MAX_DATE_MS = 8.64e15
private const val MAX_DATE_SECONDS = 8_640_000_000_000L

// Intl matches zone ids case-insensitively, java.time does not. Reject case variants of known ids, so the browser
// accepts no id the JVM would reject.
private val supportedIdsByLowercase: Map<String, String> by lazy { MpTimezone.supportedIds.associateBy { it.lowercase() } }

// One interval cache per zone id that resolved; Intl is far too slow to ask on every conversion.
private val offsetCaches: dynamic = js("Object.create(null)")

internal actual fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Long): Int {
    return offsetCacheFor(zoneId).offsetSecondsAt(epochSeconds)
}

private fun offsetCacheFor(zoneId: String): OffsetIntervalCache {
    val cached = offsetCaches[zoneId]
    if (cached != null && cached != undefined) return cached.unsafeCast<OffsetIntervalCache>()

    // Validates the id first, so unknown ids never get a cache.
    formatterFor(zoneId)

    val cache = OffsetIntervalCache(domain = -MAX_DATE_SECONDS..MAX_DATE_SECONDS) { epochSeconds ->
        intlOffsetSecondsAt(zoneId, epochSeconds)
    }
    offsetCaches[zoneId] = cache
    return cache
}

/** The offset of [zoneId] at [epochSeconds], from Intl's wall-clock rendering. */
private fun intlOffsetSecondsAt(zoneId: String, epochSeconds: Long): Int {
    // Intl renders to whole seconds, so ask about whole seconds — otherwise sub-second instants
    // (e.g. ...:59.999Z) skew the offset by up to a second.
    val flooredMs = epochSeconds.toDouble() * 1000.0

    require(flooredMs in -MAX_DATE_MS..MAX_DATE_MS) { "Instant at $epochSeconds s is outside the range of JS dates" }

    val parts = formatterFor(zoneId).formatToParts(Date(flooredMs))
    val m: dynamic = js("({})")
    val len: Int = parts.length
    for (i in 0 until len) {
        val part = parts[i]
        m[part.type] = part.value
    }

    var hour: Int = (m.hour as String).toInt()
    if (hour == 24) hour = 0 // some engines render midnight as "24"

    // Reconstruct the wall-clock instant as UTC via setUTCFullYear/Hours. Using Date.UTC() would
    // remap 2-digit years (0-99) to 1900-1999, breaking ancient dates (e.g. year 39).
    val d = Date(0.0)
    val dd = d.asDynamic()
    dd.setUTCFullYear(wallYear(m), (m.month as String).toInt() - 1, (m.day as String).toInt())
    dd.setUTCHours(hour, (m.minute as String).toInt(), (m.second as String).toInt(), 0)
    val asUtc: Double = d.getTime()

    return round((asUtc - flooredMs) / 1000.0).toInt()
}

// Read once: building an Intl.DateTimeFormat per access is expensive, and the JVM's ZoneId.systemDefault() also keeps
// the zone the process started with. A tab picks up an OS zone change on reload.
private val systemZoneId: String by lazy {
    val id: dynamic = intl.DateTimeFormat().resolvedOptions().timeZone

    if (id == null || id == undefined) "UTC" else id as String
}

internal actual fun platformSystemDefaultZoneId(): String = systemZoneId

/** The proleptic year: en-US renders years before 1 AD as "1 BC" style era years. */
private fun wallYear(m: dynamic): Int {
    val year = (m.year as String).toInt()

    return if (m.era == "BC" || m.era == "B") 1 - year else year
}

private fun formatterFor(zoneId: String): dynamic {
    val cached = formatterCache[zoneId]
    if (cached != null && cached != undefined) return cached

    val opts: dynamic = js(
        "({ hour12: false, era: 'short', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })"
    )
    opts.timeZone = zoneId

    supportedIdsByLowercase[zoneId.lowercase()]?.let { known ->
        if (known != zoneId) throw IllegalTimeZoneException("Unknown time zone: $zoneId (ids are case-sensitive: $known)")
    }

    val f = try {
        intl.DateTimeFormat("en-US", opts)
    } catch (e: Throwable) {
        // Intl throws a RangeError for an unknown zone id
        throw IllegalTimeZoneException("Unknown time zone: $zoneId", e)
    }

    formatterCache[zoneId] = f
    return f
}
