package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamMapper

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
