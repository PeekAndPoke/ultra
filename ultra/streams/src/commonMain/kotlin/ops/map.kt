package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamMapper

/**
 * Maps incoming values from type [IN] to type [OUT].
 */
fun <IN, OUT> Stream<IN>.map(mapper: (IN) -> OUT): Stream<OUT> {
    return StreamMapper(this, mapper)
}
