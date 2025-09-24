package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamWrapper

fun <T> Stream<T>.onEach(block: (T) -> Unit): Stream<T> =
    StreamOnEachOperator(wrapped = this, block = block)

private class StreamOnEachOperator<T>(
    wrapped: Stream<T>,
    private val block: (T) -> Unit,
) : StreamWrapper<T>(wrapped = wrapped) {

    override fun handleIncoming(value: T) {

        block(value)

        super.handleIncoming(value)
    }
}
