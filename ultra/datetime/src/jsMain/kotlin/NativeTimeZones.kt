@file:Suppress("unused", "UnsafeCastFromDynamic")

package io.peekandpoke.ultra.datetime

import io.peekandpoke.ultra.datetime.JsJodaCore.ZoneRulesProvider
import kotlin.js.Date
import kotlin.math.floor
import kotlin.math.round

/**
 * Native, zero-data timezone support for `kotlinx-datetime` on Kotlin/JS.
 *
 * Instead of bundling the IANA database (`@js-joda/timezone`, ~700 KB), this registers a custom
 * js-joda [ZoneRulesProvider] that resolves offsets from the browser's own IANA database via
 * [ Intl.DateTimeFormat ]. Call [installNativeTimezones] once at startup, before any time zone is
 * resolved.
 *
 * Approach: `instant -> offset` is read directly from `Intl`; `localDateTime -> offset` (which must
 * handle DST gaps and overlaps) is derived by binary-searching for the nearby transition, then the
 * resulting rules object is handed to js-joda, which performs the actual date math.
 */

@JsModule("@js-joda/core")
@JsNonModule
private external object JsJodaCore {
    val ZoneOffset: dynamic
    val ZoneOffsetTransition: dynamic
    val LocalDateTime: dynamic
    val ZoneRulesProvider: dynamic
    val DateTimeException: dynamic
}

private const val DAY_MS = 86_400_000.0

private val intl: dynamic = js("Intl")
private val formatterCache: dynamic = js("({})")
private val rulesCache: dynamic = js("({})")
private var installed = false

/** Registers the Intl-backed [ZoneRulesProvider]. Idempotent. */
fun installNativeTimezones() {
    if (installed) return

    val provider = ZoneRulesProvider

    provider.getRules = fun(zoneId: String): dynamic {
        return try {
            rulesFor(zoneId)
        } catch (t: Throwable) {
            throw JsJodaCore.DateTimeException("Unknown or unsupported time-zone ID: $zoneId")
        }
    }

    provider.getAvailableZoneIds = fun(): dynamic {
        val supported = intl.supportedValuesOf
        return if (supported != undefined) intl.supportedValuesOf("timeZone") else arrayOf("UTC")
    }

    installed = true
}

private fun formatterFor(zoneId: String): dynamic {
    val cached = formatterCache[zoneId]
    if (cached != null && cached != undefined) return cached

    val opts: dynamic =
        js("({ hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })")
    opts.timeZone = zoneId

    // Throws RangeError for an unknown zone id — surfaced as DateTimeException by getRules.
    val f = intl.DateTimeFormat("en-US", opts)
    formatterCache[zoneId] = f
    return f
}

/** Offset (in seconds) of [zoneId] at the given epoch-millis, via Intl wall-clock difference. */
private fun offsetSecondsAt(zoneId: String, epochMillis: Double): Int {
    // Intl renders to whole seconds, so compare against a second-floored instant — otherwise
    // sub-second instants (e.g. ...:59.999Z) skew the offset by up to a second.
    val flooredMs = floor(epochMillis / 1000.0) * 1000.0

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
    dd.setUTCFullYear((m.year as String).toInt(), (m.month as String).toInt() - 1, (m.day as String).toInt())
    dd.setUTCHours(hour, (m.minute as String).toInt(), (m.second as String).toInt(), 0)
    val asUtc: Double = d.getTime()

    return round((asUtc - flooredMs) / 1000.0).toInt()
}

private class LocalInfo(
    val type: String, // "normal" | "gap" | "overlap"
    val offset: dynamic,
    val before: dynamic,
    val after: dynamic,
    val transitionLdt: dynamic,
)

/** Resolves how a wall-clock [ldt] (a js-joda LocalDateTime) maps to offset(s) in [zoneId]. */
private fun localInfo(zoneId: String, ldt: dynamic): LocalInfo {
    val zoneOffset = JsJodaCore.ZoneOffset
    val ldtSec: Double = (ldt.toEpochSecond(zoneOffset.UTC) as Double)
    val ldtMs = ldtSec * 1000.0

    val offBefore = offsetSecondsAt(zoneId, ldtMs - DAY_MS)
    val offAfter = offsetSecondsAt(zoneId, ldtMs + DAY_MS)

    if (offBefore == offAfter) {
        return LocalInfo("normal", zoneOffset.ofTotalSeconds(offBefore), null, null, null)
    }

    // A transition exists within +/- 1 day — binary-search for its instant (to the second).
    var lo = ldtMs - DAY_MS
    var hi = ldtMs + DAY_MS
    while (hi - lo > 1000.0) {
        val mid = floor((lo + hi) / 2.0)
        if (offsetSecondsAt(zoneId, mid) == offBefore) lo = mid else hi = mid
    }
    val transSec = floor(hi / 1000.0)

    val before = zoneOffset.ofTotalSeconds(offBefore)
    val after = zoneOffset.ofTotalSeconds(offAfter)
    val transitionLdt = JsJodaCore.LocalDateTime.ofEpochSecond(transSec, 0, before)

    // Wall-clock seconds on each side of the transition.
    val wallBefore = transSec + offBefore
    val wallAfter = transSec + offAfter

    if (offAfter > offBefore) {
        // Spring forward → gap: local times in [wallBefore, wallAfter) do not exist.
        if (ldtSec >= wallBefore && ldtSec < wallAfter) {
            return LocalInfo("gap", null, before, after, transitionLdt)
        }
    } else {
        // Fall back → overlap: local times in [wallAfter, wallBefore) occur twice.
        if (ldtSec >= wallAfter && ldtSec < wallBefore) {
            return LocalInfo("overlap", null, before, after, transitionLdt)
        }
    }

    val off = if (ldtSec < minOf(wallBefore, wallAfter)) offBefore else offAfter
    return LocalInfo("normal", zoneOffset.ofTotalSeconds(off), null, null, null)
}

private fun rulesFor(zoneId: String): dynamic {
    val cached = rulesCache[zoneId]
    if (cached != null && cached != undefined) return cached

    // Validate the zone id eagerly (throws for unknown zones).
    formatterFor(zoneId)

    val zoneOffset = JsJodaCore.ZoneOffset
    val rules: dynamic = js("({})")
    rules._zoneId = zoneId

    rules.isFixedOffset = fun(): Boolean = false

    rules.offsetOfInstant = fun(instant: dynamic): dynamic =
        zoneOffset.ofTotalSeconds(offsetSecondsAt(zoneId, instant.toEpochMilli() as Double))

    rules.offsetOfEpochMilli = fun(epochMilli: dynamic): dynamic =
        zoneOffset.ofTotalSeconds(offsetSecondsAt(zoneId, epochMilli as Double))

    rules.offsetOfLocalDateTime = fun(ldt: dynamic): dynamic {
        val info = localInfo(zoneId, ldt)
        return when (info.type) {
            "gap" -> info.after
            "overlap" -> info.before
            else -> info.offset
        }
    }

    rules.offset = fun(arg: dynamic): dynamic =
        if (arg.toEpochMilli != undefined) rules.offsetOfInstant(arg) else rules.offsetOfLocalDateTime(arg)

    rules.validOffsets = fun(ldt: dynamic): dynamic {
        val info = localInfo(zoneId, ldt)
        return when (info.type) {
            "gap" -> arrayOf<dynamic>()
            "overlap" -> arrayOf(info.before, info.after)
            else -> arrayOf(info.offset)
        }
    }

    rules.transition = fun(ldt: dynamic): dynamic {
        val info = localInfo(zoneId, ldt)
        return if (info.type == "normal") null
        else JsJodaCore.ZoneOffsetTransition.of(info.transitionLdt, info.before, info.after)
    }

    rules.equals = fun(other: dynamic): Boolean =
        other != null && other != undefined && other._zoneId == zoneId

    rules.toString = fun(): String = zoneId

    rulesCache[zoneId] = rules
    return rules
}
