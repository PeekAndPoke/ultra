package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamWrapper

/**
 * Ignores duplicate values and thus only publishes when the value has changed.
 *
 * By default, the comparison between the previous and current value is not strict ==.
 * You can set [strict] to true to use strict === comparison
 */
fun <T> Stream<T>.distinct(strict: Boolean = false): Stream<T> =
    StreamDistinctOperator(wrapped = this, strict = strict)

/**
 * Ignores duplicate values and thus only publishes when the value has changed.
 *
 * The comparison between the previous and current value is NOT strict, using ==.
 */
fun <T> Stream<T>.distinct(): Stream<T> =
    StreamDistinctOperator(wrapped = this, strict = false)

/**
 * Ignores duplicate values and thus only publishes when the value has changed.
 *
 * The comparison between the previous and current value IS strict, using ===.
 */
fun <T> Stream<T>.distinctStrict(): Stream<T> =
    StreamDistinctOperator(wrapped = this, strict = true)

/**
 * Operator impl
 */
private class StreamDistinctOperator<T>(
    wrapped: Stream<T>,
    private val strict: Boolean,
) : StreamWrapper<T>(wrapped = wrapped) {

    private var latest: T = wrapped()

    override fun invoke(): T = latest

    override fun handleIncoming(value: T) {

        val accept = when (strict) {
            true -> value !== latest
            else -> value != latest
        }

        latest = value

        if (accept) {
            super.handleIncoming(value)
        }
    }
}
