package io.peekandpoke.kraft.addons.chartjs

import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Polls until [check] passes or [timeout] is reached. */
internal suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = Clock.System.now().plus(timeout)

    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (Clock.System.now() > deadline) throw e
            delay(poll)
        }
    }
}
