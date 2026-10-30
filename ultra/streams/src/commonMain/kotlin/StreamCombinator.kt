package io.peekandpoke.ultra.streams

/**
 * Operator that combines two streams.
 */
class StreamCombinator<FIRST, SECOND, RESULT>(
    /** The first wrapped stream */
    private val first: Stream<FIRST>,
    /** The second wrapped stream */
    private val second: Stream<SECOND>,
    /** The function that combines both */
    private val combine: (FIRST, SECOND) -> RESULT,
) : Stream<RESULT> {

    /** Unsubscribe function for the subscription on the first wrapped stream */
    private var firstUnsubscribe: Unsubscribe? = null

    /** Unsubscribe function for the subscription on the second wrapped stream */
    private var secondUnsubscribe: Unsubscribe? = null

    /** All subscriptions to this stream */
    private val subscriptions = mutableSetOf<StreamHandler<RESULT>>()

    override fun invoke(): RESULT {
        return combine(first(), second())
    }

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


        // Subscribe to the first wrapped stream if necessary
        try {
            if (firstUnsubscribe == null) {
                firstUnsubscribe = first.subscribeToStream {
                    handleInComing()
                }
            }

            // Subscribe to the second wrapped stream if necessary
            if (secondUnsubscribe == null) {
                secondUnsubscribe = second.subscribeToStream {
                    handleInComing()
                }
            }

            this.subscriptions.add(entry)

            sub(invoke())
        } catch (e: Throwable) {
            // No unsubscribe reaches the caller: the handler must not stay behind, nor the wrapped subscriptions
            subscriptions.remove(entry)
            releaseIfUnused()
            throw e
        }

        return {
            subscriptions.remove(entry)
            releaseIfUnused()
        }
    }

    /**
     * Handles new incoming data
     */
    /** When there are no subscriptions left we unsubscribe from the wrapped streams. */
    private fun releaseIfUnused() {
        if (subscriptions.isEmpty()) {
            firstUnsubscribe?.invoke()
            firstUnsubscribe = null

            secondUnsubscribe?.invoke()
            secondUnsubscribe = null
        }
    }

    private fun handleInComing() {
        publish(
            invoke()
        )
    }

    /**
     * Publishes the next value to all subscribers.
     *
     * A failing handler will not prevent other handlers from being notified.
     */
    private fun publish(value: RESULT) {
        notifyHandlers(subscriptions, value)
    }
}
