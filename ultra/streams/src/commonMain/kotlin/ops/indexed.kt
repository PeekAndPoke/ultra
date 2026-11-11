package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamWrapperBase

/**
 * Map the stream to pairs of index to value.
 *
 * The first index is 0. Values are counted while someone listens; read meanwhile, the stream gives the source's value
 * with the index the next value gets, and counts nothing (ULTRA-00302).
 */
fun <T> Stream<T>.indexed(): Stream<Pair<Int, T>> = StreamIndexedOperator(this)

private class StreamIndexedOperator<T>(
    private val wrapped: Stream<T>,
) : StreamWrapperBase<T, Pair<Int, T>>(wrapped = wrapped) {

    private var next = 0

    private var latest: Pair<Int, T>? = null

    override fun invoke(): Pair<Int, T> = latest?.takeIf { isSubscribedToWrapped() } ?: Pair(next, wrapped())

    override fun handleIncoming(value: T) {
        val indexed = Pair(next++, value)
        latest = indexed
        publish(indexed)
    }
}
