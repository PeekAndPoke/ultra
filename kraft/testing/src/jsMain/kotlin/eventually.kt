package io.peekandpoke.kraft.testing

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Runs [check] until it passes, polling every [poll]; rethrows the last failure after [timeout].
 *
 * [check] may suspend, so KQuery selections work inside it.
 */
suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = TimeSource.Monotonic.markNow() + timeout

    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (deadline.hasPassedNow()) throw e
            delay(poll)
        }
    }
}
