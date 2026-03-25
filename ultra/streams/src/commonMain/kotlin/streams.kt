package io.peekandpoke.ultra.streams

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
 * Notifies all [handlers] with the given [value].
 *
 * Uses a snapshot (toList) to avoid concurrent modification, and catches exceptions
 * per handler so that a failing subscriber does not prevent others from being notified.
 */
internal fun <T> notifyHandlers(handlers: Set<StreamHandler<T>>, value: T) {
    handlers.toList().forEach {
        try {
            it(value)
        } catch (t: Throwable) {
            // A failing handler must not prevent other handlers from being notified.
            // But we log the error so it doesn't go unnoticed during development.
            println("[Streams] ERROR: subscriber threw ${t::class.simpleName}: ${t.message}")
        }
    }
}

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
