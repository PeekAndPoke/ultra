package io.peekandpoke.ultra.datetime

import kotlin.time.Duration

/**
 * Runs [block] and fails when it does not finish within [limit], so a regression to an endless loop fails instead of
 * hanging the suite. Only the JVM enforces the limit; JS and native run [block] directly.
 */
expect fun <T> withinTimeLimit(limit: Duration, block: () -> T): T
