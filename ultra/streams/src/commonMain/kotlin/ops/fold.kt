package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamWrapperBase

/**
 * Folds the incoming values with the [operation] starting with the [initial] value.
 *
 * Values are folded only while someone listens, and each new subscription folds in the source's current value again,
 * also one folded before (ULTRA-00302). Read meanwhile, the stream gives what it folded so far.
 */
fun <T, R> Stream<T>.fold(initial: R, operation: (acc: R, next: T) -> R): Stream<R> =
    StreamFoldOperator(
        wrapped = this,
        initial = initial,
        predicate = { true },
        operation = operation,
    )

fun <T, R> Stream<T?>.foldNotNull(initial: R, operation: (acc: R, next: T) -> R): Stream<R> =
    StreamFoldOperator(
        wrapped = this,
        initial = initial,
        predicate = { it != null },
        operation = operation,
    )

/**
 * Operator impl
 */
private class StreamFoldOperator<IN, V : IN, R>(
    wrapped: Stream<IN>,
    initial: R,
    private val predicate: (next: IN) -> Boolean,
    private val operation: (acc: R, next: V) -> R,
) : StreamWrapperBase<IN, R>(
    wrapped = wrapped
) {
    private var current: R = initial

    override fun invoke(): R = current

    override fun handleIncoming(value: IN) {
        if (predicate(value)) {
            @Suppress("UNCHECKED_CAST")
            current = operation(current, value as V)
            publish(current)
        }
    }
}
