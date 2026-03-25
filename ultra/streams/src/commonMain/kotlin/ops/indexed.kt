package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamMapper

/**
 * Map the stream to pairs of index to value.
 *
 * The first index is 0.
 */
fun <T> Stream<T>.indexed(): Stream<Pair<Int, T>> {

    var idx = 0

    return StreamMapper(
        wrapped = this,
        mapper = { Pair(idx++, it) },
        initial = { Pair(0, this()) }
    )
}
