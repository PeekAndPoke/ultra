package io.peekandpoke.kraft.testing

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Runs [check] until it passes, polling every [poll], and returns the value of the passing run; rethrows the last
 * failure after [timeout].
 *
 * [check] may suspend, so KQuery selections work inside it: `val stage = eventually { findStage().shouldNotBeNull() }`.
 */
suspend fun <T> eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> T,
): T {
    val deadline = TimeSource.Monotonic.markNow() + timeout

    while (true) {
        try {
            return check()
        } catch (e: Throwable) {
            if (deadline.hasPassedNow()) throw e
            delay(poll)
        }
    }
}
