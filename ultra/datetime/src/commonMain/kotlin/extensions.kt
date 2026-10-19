package io.peekandpoke.ultra.datetime

import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

/**
 * Get the offset in total milliseconds
 */
val UtcOffset.totalMillis: Long
    get() {
        return totalSeconds * 1000L
    }

/**
 * Converts the [TimeZone] into an [MpTimezone]
 */
val TimeZone.mp: MpTimezone get() = MpTimezone.of(id)

/**
 * Get the offset at the given [instant].
 */
fun TimeZone.offsetAt(instant: MpInstant): UtcOffset {
    return mp.offsetAt(instant)
}

/**
 * Get the offset in total seconds at the given [instant].
 */
fun TimeZone.offsetSecondsAt(instant: MpInstant): Int {
    return mp.offsetSecondsAt(instant)
}

/**
 * Get the offset in total milliseconds at the given [instant].
 */
fun TimeZone.offsetMillisAt(instant: MpInstant): Long {
    return mp.offsetMillisAt(instant)
}

/**
 * Get the offset at the given [instant].
 */
fun MpTimezone.offsetAt(instant: MpInstant): UtcOffset {
    val total = offsetSecondsAt(instant)

    return UtcOffset(hours = total / 3600, minutes = (total % 3600) / 60, seconds = total % 60)
}

/**
 * Get the offset in total seconds at the given [instant].
 */
fun MpTimezone.offsetSecondsAt(instant: MpInstant): Int {
    return TimezoneEngine.offsetSecondsAt(this, instant.value)
}

/**
 * Get the offset in total milliseconds at the given [instant].
 */
fun MpTimezone.offsetMillisAt(instant: MpInstant): Long {
    return offsetSecondsAt(instant) * 1000L
}
