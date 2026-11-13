package io.peekandpoke.ultra.datetime

import kotlin.time.Duration

/**
 * Runs [block] and fails when it does not finish within [limit], so a regression to an endless loop fails instead of
 * hanging the suite. Only the JVM enforces the limit; JS and native run [block] directly.
 *
 * On the JVM a block that overruns is interrupted, but a busy loop ignores that: it keeps running (and allocating) on
 * its daemon thread until it ends, fails, e.g. with an OutOfMemoryError, or the test JVM exits.
 */
expect fun <T> withinTimeLimit(limit: Duration, block: () -> T): T
