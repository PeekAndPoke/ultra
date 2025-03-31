package de.peekandpoke.kraft.utils

import kotlinx.browser.window
import org.w3c.dom.Window


/**
 * Helper function for a nicer use of [Window.setTimeout] returning an Int.
 *
 * @return The timer id
 */
fun setTimeout(timeMs: Int, block: () -> Unit): Int {
    return window.setTimeout(block, timeMs)
}

/**
 * Helper function for a nicer use of [Window.clearTimeout].
 */
fun clearTimeout(timerId: Int) {
    window.clearTimeout(timerId)
}

/**
 * Helper function for a nicer use of [Window.setInterval] returning an Int.
 *
 * @return The timer id
 */
fun setInterval(timeMs: Int, block: () -> Unit): Int {
    return window.setInterval(block, timeMs)
}

/**
 * Helper function for a nicer use of [Window.clearInterval].
 */
fun clearInterval(timerId: Int) {
    window.clearInterval(timerId)
}
