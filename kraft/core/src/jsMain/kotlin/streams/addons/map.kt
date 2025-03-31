package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamMapper

/**
 * Maps incoming values from type [IN] to type [OUT].
 */
fun <IN, OUT> Stream<IN>.map(mapper: (IN) -> OUT): Stream<OUT> {
    return StreamMapper(this, mapper)
}
