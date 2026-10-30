package io.peekandpoke.ultra.streams

/**
 * Base class for all stream wrappers
 */
abstract class StreamWrapperBase<WRAPPED, RESULT>(
    /** The wrapped stream */
    private val wrapped: Stream<WRAPPED>,
) : Stream<RESULT> {

    /** Unsubscribe function for the subscription on the wrapped stream */
    private var wrappedUnsubscribe: Unsubscribe? = null

    /** All subscriptions to this stream */
    private val subscriptions = mutableSetOf<StreamHandler<RESULT>>()

    /**
     * Returns 'true' if the stream has subscriptions
     */
    protected fun isSubscribedToWrapped(): Boolean {
        return wrappedUnsubscribe != null
    }

    /**
     * Handles the next incoming value of the wrapped stream
     */
    protected abstract fun handleIncoming(value: WRAPPED)

    /**
     * Adds a subscription to the stream.
     *
     * On subscribing the subscription is immediately called with the current value.
     *
     * Returns an unsubscribe function. Calling this function unsubscribes from the stream.
     *
     * NOTICE: It is the callers' obligation to unsubscribe from the stream.
     */
    override fun subscribeToStream(sub: StreamHandler<RESULT>): Unsubscribe {
        // An entry of this subscription's own: one handler given twice (a lambda that captures nothing is one shared
        // instance) is two subscriptions, and the first to leave must not take the other's entry with it
        val entry = ownEntry(sub)


        // Subscribe to the wrapped stream if necessary
        if (wrappedUnsubscribe == null) {
            wrappedUnsubscribe = wrapped.subscribeToStream {
                handleIncoming(it)
            }
        }

        this.subscriptions.add(entry)

        try {
            sub(invoke())
        } catch (e: Throwable) {
            // No unsubscribe reaches the caller: the handler must not stay behind, nor the wrapped subscription
            subscriptions.remove(entry)

            if (subscriptions.isEmpty()) {
                wrappedUnsubscribe?.invoke()
                wrappedUnsubscribe = null
            }

            throw e
        }

        return {
            subscriptions.remove(entry)

            // Unsubscribe from the wrapped stream if necessary
            if (subscriptions.isEmpty()) {
                wrappedUnsubscribe?.invoke()
                wrappedUnsubscribe = null
            }
        }
    }

    /**
     * Publishes the next value to all subscribers.
     *
     * A failing handler will not prevent other handlers from being notified.
     */
    protected fun publish(value: RESULT) {
        notifyHandlers(subscriptions, value)
    }
}
