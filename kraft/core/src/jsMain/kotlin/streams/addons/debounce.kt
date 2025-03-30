package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamWrapper
import de.peekandpoke.kraft.utils.DebouncingTimer

/**
 * Debounce the incoming values by the given [delayMs]
 */
fun <T> Stream<T>.debounce(
    delayMs: Int,
    delayFirstMs: Int = 3,
): Stream<T> = StreamDebounceOperator(this, delayMs, delayFirstMs)

fun debouncedFunc(delayMs: Int = 200, block: () -> Unit): () -> Unit {
    val timer = DebouncingTimer(delayMs)

    return {
        timer.invoke(block)
    }
}

fun debouncedFuncExceptFirst(delayMs: Int = 200, block: () -> Unit): () -> Unit {
    val timer = DebouncingTimer(delayMs = delayMs, delayFirstMs = 3)

    return {
        timer.invoke(block)
    }
}

/**
 * Implementation of a debouncing stream
 */
private class StreamDebounceOperator<T>(
    wrapped: Stream<T>,
    delayMs: Int,
    delayFirstMs: Int,
) : StreamWrapper<T>(wrapped) {

    private val debounce = DebouncingTimer(delayMs, delayFirstMs)

    override fun handleIncoming(value: T) {
        debounce {
            super.handleIncoming(value)
        }
    }
}
