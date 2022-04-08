package de.peekandpoke.ultra.common.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt as instantOffsetAt

/**
 * Get the offset in total milliseconds
 */
// TODO: test
val UtcOffset.totalMillis: Long
    get() {
        return totalSeconds * 1000L
    }

/**
 * Get the offset at the given [instant].
 */
// TODO: test me
fun TimeZone.offsetAt(instant: MpInstant): UtcOffset {
    return instantOffsetAt(instant.value)
}

/**
 * Get the offset at the given [instant].
 */
// TODO: test me
fun MpTimezone.offsetAt(instant: MpInstant): UtcOffset {
    return kotlinx.offsetAt(instant)
}

/**
 * Get the offset in total seconds at the given [instant].
 */
// TODO: test me
fun TimeZone.offsetSecondsAt(instant: MpInstant): Int {
    return offsetAt(instant).totalSeconds
}

/**
 * Get the offset in total seconds at the given [instant].
 */
// TODO: test me
fun MpTimezone.offsetSecondsAt(instant: MpInstant): Int {
    return offsetAt(instant).totalSeconds
}

/**
 * Get the offset in total milliseconds at the given [instant].
 */
// TODO: test me
fun TimeZone.offsetMillisAt(instant: MpInstant): Long {
    return offsetAt(instant).totalMillis
}

/**
 * Get the offset in total milliseconds at the given [instant].
 */
// TODO: test me
fun MpTimezone.offsetMillisAt(instant: MpInstant): Long {
    return offsetAt(instant).totalMillis
}
