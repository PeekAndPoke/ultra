package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.AsyncStreamWrapper
import de.peekandpoke.ultra.streams.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
) : AsyncStreamWrapper<IN, OUT>(
    wrapped = wrapped,
    mapper = mapper,
    scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
)
