package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.AsyncStreamWrapper
import de.peekandpoke.kraft.streams.Stream

/**
 * Maps incoming values asynchronously from type [IN] to type [OUT].
 *
 * Notice that the result of the [mapper] can be null.
 * In this case a 'null' will be published.
 */
fun <IN, OUT> Stream<IN>.mapAsync(mapper: suspend (IN) -> OUT?): Stream<OUT?> =
    StreamMapAsyncOperator(this, mapper)

/**
 * Operator impl for [mapAsync]
 */
private class StreamMapAsyncOperator<IN, OUT>(
    wrapped: Stream<IN>,
    mapper: suspend (IN) -> OUT?,
) : AsyncStreamWrapper<IN, OUT>(wrapped, mapper)
