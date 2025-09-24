package de.peekandpoke.ultra.streams.ops

import kotlinx.browser.window

/**
 * Helper class implementing a debouncing timer.
 *
 * The block() passed to [schedule] will only be executed after [delayMs]
 * if no other call to [schedule] occurred in the meanwhile.
 */
class DebouncingTimer(private val delayMs: Int, private val delayFirstMs: Int = delayMs) {

    private var counter = 0
    private var timerId: Int? = null

    /**
     * Schedules [block] to be called after the timeout.
     */
    operator fun invoke(block: () -> Unit) {
        if (counter++ == 0) {
            schedule(delayFirstMs, block)
        } else {
            schedule(delayMs, block)
        }
    }

    /**
     * Schedules the next timeout.
     */
    private fun schedule(delay: Int, block: () -> Unit) {
        timerId?.let {
            window.clearTimeout(it)
        }

        timerId = window.setTimeout({
            timerId = null
            block()
        }, delay)
    }
}
