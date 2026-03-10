package de.peekandpoke.ultra.streams.ops

import de.peekandpoke.ultra.streams.Stream

/**
 * Publishes the [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackTo(fallback: T): Stream<T> {
    return map { it ?: fallback }
}

/**
 * Publishes the value produced by [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackBy(fallback: () -> T): Stream<T> {
    return map { it ?: fallback() }
}
