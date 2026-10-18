package io.peekandpoke.kraft.addons.datetime

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.KraftApp.AppInitializer

/**
 * Opt-in time zone data for RAW `kotlinx-datetime` zones on Kotlin/JS.
 *
 * Not needed for ultra's `Mp*` types (`MpTimezone`, `MpZonedDateTime`, ...): they resolve zones through the
 * browser's own IANA database via `Intl` and bundle zero data. Only code that calls kotlinx directly, e.g.
 * `TimeZone.of("Europe/Berlin")`, needs ONE of these loaders, called once at startup before any zone is resolved.
 * Without one, kotlinx only knows UTC and fixed offsets.
 *
 * Each loader bundles a prebuilt js-joda slice (call only the one you need — dead-code elimination keeps just
 * that dataset):
 *
 * - [installFullTimezones]          ~713 / 37 KB — all zones, full history and future
 * - [installTimezones1970to2030]    ~130 / 18 KB — for typical business apps
 * - [installTimezones10YearRange]    ~40 / 10 KB — smallest; ~±5 years around build time only
 */

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
// Load a dataset at app start, e.g. `kraftApp { fullTimezones() }`. Each extension references
// only its own dataset, so DCE bundles just the one you call.

private fun tzInitializer(install: () -> Unit) = object : AppInitializer {
    override val initPriority: Int = Int.MAX_VALUE
    override fun initialize() = install()
}

/** Full js-joda IANA dataset (~713 KB / ~37 KB gzip). */
fun KraftApp.Builder.fullTimezones() = timezones(tzInitializer { installFullTimezones() })

/** js-joda 1970–2030 dataset (~130 KB / ~18 KB gzip). */
fun KraftApp.Builder.timezones1970to2030() = timezones(tzInitializer { installTimezones1970to2030() })

/** js-joda ~10-year-range dataset (~40 KB / ~10 KB gzip). */
fun KraftApp.Builder.timezones10YearRange() = timezones(tzInitializer { installTimezones10YearRange() })
