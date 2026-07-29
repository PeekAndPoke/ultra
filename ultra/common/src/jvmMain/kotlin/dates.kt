package io.peekandpoke.ultra.common

import java.util.*

/**
 * Returns a new [Date] that is [minutes] minutes after this date.
 *
 * Pure epoch-millis arithmetic: no calendar and no time zone are involved, so a minute is always
 * 60 seconds, even across a dst switch. Pass a negative value to go back in time.
 */
fun Date.plusMinutes(minutes: Long) = Date(this.time + minutes * 60 * 1000)
