package io.peekandpoke.ultra.datetime

/**
 * The instant, in epoch milliseconds, up to which this platform's zone engine resolves a REGION zone, or null when it
 * has no such limit (JVM, native: only the date types' own range). JS: 8.64e15 ms, the range of `Date`.
 */
expect val zoneEngineLimitMillis: Long?

/** True on native: its zone engine stops a day inside each end of the date range (kotlinx-native's own limit). */
expect val isNativePlatform: Boolean
