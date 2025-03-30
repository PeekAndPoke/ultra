package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream

/**
 * Published the [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackTo(fallback: T): Stream<T> {
    return map { it ?: fallback }
}

/**
 * Published the value produced by [fallback] whenever the value on the stream is null.
 */
fun <T> Stream<T?>.fallbackBy(fallback: () -> T): Stream<T> {
    return map { it ?: fallback() }
}
