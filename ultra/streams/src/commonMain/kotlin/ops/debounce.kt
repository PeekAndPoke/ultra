package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounce the incoming values by the given [delayMs].
 *
 * Only the last value received within the delay window is published.
 */
fun <T> Stream<T>.debounce(delayMs: Int): Stream<T> =
    StreamDebounceOperator(this, delayMs)

/**
 * Implementation of a debouncing stream using coroutines.
 */
private class StreamDebounceOperator<T>(
    wrapped: Stream<T>,
    private val delayMs: Int,
) : StreamWrapper<T>(wrapped) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    override fun handleIncoming(value: T) {
        job?.cancel()
        job = scope.launch {
            delay(delayMs.toLong())
            super.handleIncoming(value)
        }
    }
}
