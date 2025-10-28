package de.peekandpoke.ultra.streams

/**
 * The [Unsubscribe] function is returned when a subscription is created on a [Stream].
 *
 * Calling this function cancels the subscription.
 */
typealias Unsubscribe = () -> Unit

/**
 * Handler-function for the incoming values of a [Stream].
 */
typealias StreamHandler<T> = (T) -> Unit

/**
 * Subscribes to the stream permanently
 *
 * NOTICE: there is no way to get rid of the subscription anymore. Use with caution.
 */
fun <T> Stream<T>.permanent(): Stream<T> = permanent { }

/**
 * Subscribes to the stream permanently
 *
 * NOTICE: there is no way to get rid of the subscription anymore. Use with caution.
 */
fun <T> Stream<T>.permanent(handler: (T) -> Unit): Stream<T> = apply { subscribeToStream(handler) }
