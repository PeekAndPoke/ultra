package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.AsyncStreamWrapper
import de.peekandpoke.ultra.streams.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Maps incoming values asynchronously from type [IN] to type [OUT].
 *
 * The result of the [mapper] can be null — in that case null is published.
 * The initial value is null until the first async mapping completes.
 */
fun <IN, OUT> Stream<IN>.mapAsync(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    mapper: suspend (IN) -> OUT?,
): Stream<OUT?> = StreamMapAsyncOperator(this, mapper, scope)

/**
 * Operator impl for [mapAsync]
 */
private class StreamMapAsyncOperator<IN, OUT>(
    wrapped: Stream<IN>,
    mapper: suspend (IN) -> OUT?,
    scope: CoroutineScope,
) : AsyncStreamWrapper<IN, OUT>(
    wrapped = wrapped,
    mapper = mapper,
    scope = scope,
)
