package io.peekandpoke.ultra.datetime

import kotlin.time.Duration

actual fun <T> withinTimeLimit(limit: Duration, block: () -> T): T = block()
