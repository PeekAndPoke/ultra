package io.peekandpoke.kraft.addons.datetime

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftApp.AppInitializer
import io.peekandpoke.ultra.datetime.installNativeTimezones as installNativeTimezonesImpl

/**
 * Opt-in timezone support for `kotlinx-datetime` on Kotlin/JS.
 *
 * `kraft:core` does NOT load any timezone data automatically. Apps that use named time zones
 * (e.g. `TimeZone.of("Europe/Berlin")`) must call exactly ONE installer below once at startup,
 * before any zone is resolved. Without it, only the SYSTEM zone with a fixed offset is available.
 *
 * Prefer [installNativeTimezones]: it uses the browser's own IANA database via `Intl` and bundles
 * **zero** data. The js-joda loaders below bundle a prebuilt slice (call only the one you need —
 * dead-code elimination keeps just that dataset):
 *
 * - [installNativeTimezones]        ~0 KB        — browser-native (Intl); recommended
 * - [installFullTimezones]          ~713 / 37 KB — all zones, full history and future
 * - [installTimezones1970to2030]    ~130 / 18 KB — for typical business apps
 * - [installTimezones10YearRange]    ~40 / 10 KB — smallest; ~±5 years around build time only
 */

/**
 * Registers the native, zero-data Intl-backed timezone provider (recommended). Works in all
 * modern browsers; for pre-`Intl` engines or historical accuracy beyond Intl, use a js-joda loader.
 */
fun installNativeTimezones() = installNativeTimezonesImpl()

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTzFull

@JsModule("@js-joda/timezone/dist/js-joda-timezone-1970-2030.js")
@JsNonModule
external object JsJodaTz1970to2030

@JsModule("@js-joda/timezone/dist/js-joda-timezone-10-year-range.js")
@JsNonModule
external object JsJodaTz10YearRange

/** Loads the full IANA database (~713 KB / ~37 KB gzip). All zones, full history and future. */
fun installFullTimezones(): JsJodaTzFull = JsJodaTzFull

/** Loads zones for the years 1970–2030 (~130 KB / ~18 KB gzip). Recommended for most apps. */
fun installTimezones1970to2030(): JsJodaTz1970to2030 = JsJodaTz1970to2030

/** Loads a ~10-year window around build time (~40 KB / ~10 KB gzip). Recent dates only. */
fun installTimezones10YearRange(): JsJodaTz10YearRange = JsJodaTz10YearRange

// ---- KraftApp.Builder DSL -------------------------------------------------------------------
// Override the default native timezone support, e.g. `kraftApp { fullTimezones() }`. Each
// extension references only its own dataset, so DCE bundles just the one you call.

private fun tzInitializer(install: () -> Unit) = object : AppInitializer {
    override val initPriority: Int = Int.MAX_VALUE
    override fun initialize() = install()
}

/** Browser-native (Intl) timezone provider — zero bundled data. (Already the Kraft default.) */
fun KraftApp.Builder.nativeTimezones() = timezones(tzInitializer { installNativeTimezones() })

/** Full js-joda IANA dataset (~713 KB / ~37 KB gzip). */
fun KraftApp.Builder.fullTimezones() = timezones(tzInitializer { installFullTimezones() })

/** js-joda 1970–2030 dataset (~130 KB / ~18 KB gzip). */
fun KraftApp.Builder.timezones1970to2030() = timezones(tzInitializer { installTimezones1970to2030() })

/** js-joda ~10-year-range dataset (~40 KB / ~10 KB gzip). */
fun KraftApp.Builder.timezones10YearRange() = timezones(tzInitializer { installTimezones10YearRange() })
