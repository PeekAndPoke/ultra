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
 * A handler entry of one subscription's own, for a stream's set of handlers (ULTRA-00274): one handler given twice (a
 * lambda that captures nothing is one shared instance) is two subscriptions, and the first to leave must not take the
 * other's entry with it.
 */
internal fun <T> ownEntry(handler: StreamHandler<T>): StreamHandler<T> = { handler(it) }

/**
 * Notifies all [handlers] with the given [value].
 *
 * Uses a snapshot (toList) to avoid concurrent modification, and catches exceptions
 * per handler so that a failing subscriber does not prevent others from being notified.
 *
 * During the notification (ULTRA-00307): a handler removed meanwhile is skipped, as its unsubscribe has returned; once
 * [superseded], a newer notification of the same publisher has reached every handler, so this one stops.
 */
internal fun <T> notifyHandlers(handlers: Set<StreamHandler<T>>, value: T, superseded: () -> Boolean) {
    for (handler in handlers.toList()) {
        if (superseded()) return
        if (handler !in handlers) continue

        try {
            handler(value)
        } catch (t: Throwable) {
            // A failing handler must not prevent other handlers from being notified.
            // But we log the error so it doesn't go unnoticed during development.
            println("[Streams] ERROR: subscriber threw ${t::class.simpleName}: ${t.message}")
        }
    }
}

/**
 * Notifies a publisher's handlers, counting the notifications: one that a handler's write overtakes stops, so no handler
 * ends on the older value (ULTRA-00307).
 */
internal class Notifier {
    private var count = 0

    fun <T> notify(handlers: Set<StreamHandler<T>>, value: T) {
        val own = ++count
        notifyHandlers(handlers, value) { count != own }
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
