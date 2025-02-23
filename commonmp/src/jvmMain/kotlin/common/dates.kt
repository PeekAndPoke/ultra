package de.peekandpoke.ultra.common

import java.util.*

fun Date.plusMinutes(minutes: Long) = Date(this.time + minutes * 60 * 1000)
