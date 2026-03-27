package io.peekandpoke.ultra.common

import java.util.*

/**
 * Returns a new [Date] that is [minutes] minutes after this date.
 */
fun Date.plusMinutes(minutes: Long) = Date(this.time + minutes * 60 * 1000)
