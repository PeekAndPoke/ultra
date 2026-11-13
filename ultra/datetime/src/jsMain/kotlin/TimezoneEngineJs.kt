@file:Suppress("UnsafeCastFromDynamic")

package io.peekandpoke.ultra.datetime

import kotlinx.datetime.IllegalTimeZoneException
import kotlin.js.Date
import kotlin.math.round

// Zone offsets come from the browser's own IANA database via Intl.DateTimeFormat — no tz data is bundled.

private val intl: dynamic = js("Intl")

// JS Date covers ±8.64e15 ms; Intl throws a RangeError outside of it.
private const val MAX_DATE_MS = 8.64e15
private const val MAX_DATE_SECONDS = 8_640_000_000_000.0

// Intl matches zone ids case-insensitively, java.time does not. Case variants of listed ids are refused, as on the
// JVM; case variants of unlisted ids, and ids java.time refuses such as `EST` or `ROC`, still resolve here.
private val supportedIdsByLowercase: Map<String, String> by lazy { MpTimezone.supportedIds.associateBy { it.lowercase() } }

// One interval cache per zone id in MpTimezone.supportedIds; Intl is far too slow to ask on every conversion. Other
// ids that resolve get only the memo slot below: Intl also accepts case variants of unlisted ids, and each would add
// an entry (ULTRA-00217). No prototype: ids like "constructor" must miss the cache, not find an Object.prototype member.
private val offsetCaches: dynamic = js("Object.create(null)")

/** The zone ids in the cache, for tests. */
internal fun cachedZoneIds(): Set<String> {
    val keys: Array<String> = js("Object.keys")(offsetCaches)

    return keys.toSet()
}

/** How many Intl formatters were built successfully, for tests. */
internal var intlFormattersBuilt: Int = 0
    private set

// One slot for the last unlisted id that resolved, keyed by the exact string: one conversion asks 3 to 5 times, a
// start of day inside a gap about 17 more. Replaced by the next unlisted id, so the caches stay bounded.
private var unlistedMemo: Pair<String, OffsetIntervalCache>? = null

/** The unlisted id in the one-slot memo, for tests. */
internal fun memoizedUnlistedZoneId(): String? = unlistedMemo?.first

internal actual fun platformOffsetSecondsAt(zoneId: String, epochSeconds: Double): Int {
    val cached = offsetCaches[zoneId]
    if (cached != null && cached != undefined) {
        return cached.unsafeCast<OffsetIntervalCache>().offsetSecondsAt(epochSeconds)
    }

    unlistedMemo?.let { (id, cache) -> if (id == zoneId) return cache.offsetSecondsAt(epochSeconds) }

    // A miss: only listed ids get a cache, any other id the memo slot.
    val cache = newOffsetCache(zoneId)

    when (zoneId) {
        in MpTimezone.supportedIds -> offsetCaches[zoneId] = cache
        else -> unlistedMemo = zoneId to cache
    }

    return cache.offsetSecondsAt(epochSeconds)
}

// Only asked from 2^52 s on, far outside the JS Date range: the Double lookup throws there, as it should.
internal actual fun platformOffsetSecondsAtExact(zoneId: String, epochSeconds: Long): Int {
    return platformOffsetSecondsAt(zoneId, epochSeconds.toDouble())
}

/** A new interval cache for [zoneId]; builds the formatter first, so an unknown id throws before a cache exists. */
private fun newOffsetCache(zoneId: String): OffsetIntervalCache {
    val formatter = newFormatter(zoneId)

    return OffsetIntervalCache(domainStart = -MAX_DATE_SECONDS, domainEnd = MAX_DATE_SECONDS) { epochSeconds ->
        intlOffsetSecondsAt(formatter, epochSeconds)
    }
}

/** The offset at [epochSeconds], from the wall-clock rendering of a [formatter] made by [newFormatter]. */
private fun intlOffsetSecondsAt(formatter: dynamic, epochSeconds: Double): Int {
    // Intl renders to whole seconds, so ask about whole seconds — otherwise sub-second instants
    // (e.g. ...:59.999Z) skew the offset by up to a second.
    val flooredMs = epochSeconds * 1000.0

    require(flooredMs in -MAX_DATE_MS..MAX_DATE_MS) { "Instant at $epochSeconds s is outside the range of JS dates" }

    val parts = formatter.formatToParts(Date(flooredMs))
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

/**
 * An Intl formatter for [zoneId]. Throws [IllegalTimeZoneException] for an unknown id or a case variant of a
 * listed one.
 */
private fun newFormatter(zoneId: String): dynamic {
    val opts: dynamic = js(
        "({ hour12: false, era: 'short', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })"
    )
    opts.timeZone = zoneId

    supportedIdsByLowercase[zoneId.lowercase()]?.let { known ->
        if (known != zoneId) {
            throw IllegalTimeZoneException(
                "Unknown time zone: ${TimezoneEngine.echo(zoneId)} (ids are case-sensitive: $known)"
            )
        }
    }

    val formatter = try {
        intl.DateTimeFormat("en-US", opts)
    } catch (e: Throwable) {
        // Intl throws a RangeError for an unknown zone id
        throw IllegalTimeZoneException("Unknown time zone: ${TimezoneEngine.echo(zoneId)}", e)
    }

    intlFormattersBuilt++

    return formatter
}
