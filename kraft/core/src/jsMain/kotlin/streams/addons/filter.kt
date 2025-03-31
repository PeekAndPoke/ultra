package de.peekandpoke.kraft.streams.addons

import de.peekandpoke.kraft.streams.Stream
import de.peekandpoke.kraft.streams.StreamWrapperBase

/**
 * Filter the incoming values and publish only the ones that match the [predicate].
 */
fun <T> Stream<T>.filter(predicate: (T) -> Boolean): Stream<T?> {
    return StreamFilterOperator(wrapped = this, predicate = predicate)
}

/**
 * Filter the incoming values and publish only the ones that match the [predicate].
 *
 * The [initial] value is published as long as only null values are published by the stream.
 */
fun <T> Stream<T>.filter(initial: T, predicate: (T) -> Boolean): Stream<T> {
    return StreamFilterWithInitialValueOperator(wrapped = this, initial = initial, predicate = predicate)
}

/**
 * Filter out all null values.
 *
 * The [initial] value is published as long as only null values are published by the stream.
 */
@Suppress("UNCHECKED_CAST")
fun <T> Stream<T?>.filterNotNull(initial: T): Stream<T> {
    return filter(initial = initial, predicate = { it != null }) as Stream<T>
}

/**
 * Filters the incoming values and publish only the ones that are an instance of [I].
 */
inline fun <T, reified I : T> Stream<T?>.filterIsInstance(): Stream<I?> {
    @Suppress("UNCHECKED_CAST")
    return filter { it is I } as Stream<I?>
}

/**
 * Filters the incoming values and publish only the ones that are an instance of [I].
 */
inline fun <T, reified I : T> Stream<T?>.filterIsInstance(initial: I): Stream<I> {
    @Suppress("UNCHECKED_CAST")
    return filter(initial) { it is I } as Stream<I>
}

/**
 * Filter operator impl
 */
private class StreamFilterOperator<T>(
    wrapped: Stream<T>,
    private val predicate: (T) -> Boolean,
) : StreamWrapperBase<T, T?>(wrapped = wrapped) {

    private var latest: T? = null

    override fun invoke(): T? = latest

    override fun handleIncoming(value: T) {
        if (predicate(value)) {
            latest = value
            publish(value)
        }
    }
}

/**
 * Filter operator impl
 */
private class StreamFilterWithInitialValueOperator<T>(
    wrapped: Stream<T>,
    initial: T,
    private val predicate: (T) -> Boolean,
) : StreamWrapperBase<T, T>(wrapped = wrapped) {

    private var latest: T = initial

    override fun invoke(): T = latest

    override fun handleIncoming(value: T) {
        if (predicate(value)) {
            latest = value
            publish(value)
        }
    }
}
