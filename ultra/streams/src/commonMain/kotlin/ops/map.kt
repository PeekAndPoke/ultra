package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamMapper

/**
 * Maps incoming values from type [IN] to type [OUT].
 */
fun <IN, OUT> Stream<IN>.map(mapper: (IN) -> OUT): Stream<OUT> {
    return StreamMapper(this, mapper)
}
