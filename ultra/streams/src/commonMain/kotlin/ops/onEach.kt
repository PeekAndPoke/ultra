package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamWrapper

/**
 * Performs the given [block] on each incoming value and then passes it through, unless the block wrote a newer value
 * upstream: that one was passed on already, the older one is not (ULTRA-00307).
 */
fun <T> Stream<T>.onEach(block: (T) -> Unit): Stream<T> =
    StreamOnEachOperator(wrapped = this, block = block)

/**
 * OnEach operator impl
 */
private class StreamOnEachOperator<T>(
    wrapped: Stream<T>,
    private val block: (T) -> Unit,
) : StreamWrapper<T>(wrapped = wrapped) {

    /** Counts the incoming values: a block that wrote upstream has had the newer value passed on already. */
    private var incoming = 0

    override fun handleIncoming(value: T) {
        val own = ++incoming

        block(value)

        // The block wrote a newer value upstream, which was passed on during it: this older one is not (ULTRA-00307)
        if (own == incoming) super.handleIncoming(value)
    }
}
