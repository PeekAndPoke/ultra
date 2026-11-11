package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.AsyncStreamWrapper
import io.peekandpoke.ultra.streams.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Maps incoming values asynchronously from type [IN] to type [OUT].
 *
 * The result of the [mapper] can be null — in that case null is published.
 * The initial value is null until the first async mapping completes.
 *
 * The latest value wins: a mapping that finishes after a newer value came is dropped, not cancelled (ULTRA-00308).
 * Values are mapped only while someone listens; read meanwhile, the stream gives the last result it published.
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
