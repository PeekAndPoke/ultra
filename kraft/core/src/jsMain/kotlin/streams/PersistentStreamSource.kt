package de.peekandpoke.kraft.streams

/**
 * A persistent stream source is able to store values for later use.
 *
 * E.g. values can be stored in the local storage. When the page is then reloaded
 * the latest value can be re-loaded from the storage and the stream will be initialized with it
 */
internal class PersistentStreamSource<T>(
    private val storage: StreamStorage<T>,
    private val wrapped: StreamSource<T>,
) : StreamSource<T> {

    init {
        try {
            // Try to load the latest value and publish it on the wrapped stream source
            storage.read()?.let {
                wrapped(it)
            }
        } catch (e: Throwable) {
            console.error("Could not read from storage", e)
        }
    }

    /**
     * The initialValue property represents the initial value of the StreamSource.
     *
     * It is obtained from the wrapped StreamSource, which provides the functionality of storing and retrieving
     * values.
     */
    override val initialValue: T
        get() = wrapped.initialValue

    /**
     * Gets all subscriptions
     */
    override val subscriptions: Set<StreamHandler<T>>
        get() = wrapped.subscriptions

    /**
     * Removes all subscriptions
     */
    override fun removeAllSubscriptions() {
        wrapped.removeAllSubscriptions()
    }

    /**
     * Getting values is forwarded to the [wrapped] stream source.
     */
    override fun invoke(): T = wrapped()

    /**
     * Incoming values are stored and forwarded to the [wrapped] stream source.
     */
    override fun invoke(next: T) {
        storage.write(next)

        return wrapped(next)
    }

    /**
     * Subscribes to the stream source.
     */
    override fun subscribeToStream(sub: (T) -> Unit): Unsubscribe = wrapped.subscribeToStream(sub)
}
