package io.peekandpoke.kraft.addons.datetime

/**
 * Opt-in loaders for the js-joda IANA timezone database used by `kotlinx-datetime` on Kotlin/JS.
 *
 * `kraft:core` does NOT load any timezone data automatically. Apps that use named time zones
 * (e.g. `TimeZone.of("Europe/Berlin")`) must call exactly ONE of the loaders below once at
 * startup, before any zone is resolved. Without it, only the SYSTEM zone with a fixed offset
 * is available.
 *
 * Each loader references a different prebuilt slice of the IANA database. Call only the one you
 * need — dead-code elimination then keeps just that dataset in the bundle.
 *
 * Approximate sizes (minified / gzip):
 * - [installFullTimezones]          ~713 KB / ~37 KB — all zones, full history and future
 * - [installTimezones1970to2030]    ~130 KB / ~18 KB — recommended for typical business apps
 * - [installTimezones10YearRange]    ~40 KB / ~10 KB — smallest; ~±5 years around build time only
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
