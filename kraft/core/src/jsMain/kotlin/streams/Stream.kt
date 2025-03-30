package de.peekandpoke.kraft.streams

/**
 * Base interface for all streams
 */
interface Stream<T> {
    /**
     * Returns the current value of the stream
     */
    operator fun invoke(): T

    /**
     * Adds a subscription to the stream.
     *
     * On subscribing the subscription is immediately called with the current value.
     *
     * Returns an unsubscribe function. Calling this function unsubscribes from the stream.
     */
    fun subscribeToStream(sub: (T) -> Unit = {}): Unsubscribe
}
