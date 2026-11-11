package io.peekandpoke.ultra.streams.ops

import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.steady

/**
 * Publishes the [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackTo(fallback: T): Stream<T> = fallbackTo(steady(fallback))

/**
 * Publishes the [fallback]'s current value whenever the value on the stream is null (ULTRA-00271): a new fallback is
 * published at once while the stream is null; while it is not, the stream's value is published again. The [fallback] is
 * listened to only while someone listens here.
 */
fun <T> Stream<T?>.fallbackTo(fallback: Stream<T>): Stream<T> = combinedWith(fallback) { value, other -> value ?: other }

/**
 * Publishes the value produced by [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackBy(fallback: () -> T): Stream<T> {
    return map { it ?: fallback() }
}
