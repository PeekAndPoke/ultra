package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamCombinator

/**
 * Combines with the [other] stream.
 *
 * The [combinator] is applied whenever this or the other stream receives the next value.
 */
fun <IN1, IN2, OUT> Stream<IN1>.combinedWith(
    other: Stream<IN2>,
    combinator: (IN1, IN2) -> OUT,
): Stream<OUT> {
    return StreamCombinator(
        first = this,
        second = other,
        combine = combinator
    )
}

/**
 * Pairs the stream with the [other] stream.
 *
 * This stream publishes a [Pair] of the current value of [this] and [other],
 * whenever [this] stream or the [other] stream published a new value.
 */
fun <IN1, IN2> Stream<IN1>.pairedWith(other: Stream<IN2>): Stream<Pair<IN1, IN2>> {
    return combinedWith(other) { first, second ->
        Pair(first, second)
    }
}
