package de.peekandpoke.kraft.streams

/**
 * Define a stream source to which new values can be written.
 */
interface StreamSource<T> : Stream<T> {

    companion object {
        operator fun <T> invoke(initial: T): StreamSource<T> = StreamSourceImpl(initial)
    }

    /**
     * The initial value of the stream source.
     */
    val initialValue: T

    /**
     * Get the readonly version of this stream.
     */
    val readonly get() = this as Stream<T>

    /**
     * The subscription the stream source has.
     */
    val subscriptions: Set<StreamHandler<T>>

    /**
     * Returns the current value of the stream
     */
    override operator fun invoke(): T

    /**
     * Sends the next value to the stream
     */
    operator fun invoke(next: T)

    /**
     * Calls the [block] with the current value of the stream and sends to return value to the stream.
     *
     * The new value is only sent when it is different from the current value.
     */
    operator fun invoke(block: (T) -> T) {
        val current = this()
        val next = block(current)

        if (current != next) {
            this(next)
        }
    }

    /**
     * Modifies the current value by calling the [block] and sends it the result as the next value.
     *
     * The [block] will have the current value of the stream as the scopes this pointer.
     */
    fun modify(block: T.() -> T): Unit = invoke(block(invoke()))

    /**
     * Resets the stream source to its initial value.
     */
    fun reset(): T {
        invoke(initialValue)

        return invoke()
    }

    /**
     * Removes all subscriptions
     */
    fun removeAllSubscriptions()
}
