package io.peekandpoke.ultra.streams

open class StreamSourceImpl<T>(initial: T) : StreamSource<T> {

    /**
     * The initial value of the stream source.
     */
    override val initialValue = initial

    /**
     * The current value of the stream
     */
    protected var current: T = initial

    /**
     * All subscriptions to the stream
     */
    override val subscriptions: MutableSet<(T) -> Unit> = mutableSetOf()

    /**
     * @see StreamSource.invoke
     */
    override operator fun invoke(): T = current

    /**
     * @see StreamSource.invoke
     */
    override operator fun invoke(next: T) {
        current = next
        notifyHandlers(subscriptions, current)
    }

    /**
     * @see StreamSource.subscribeToStream
     */
    override fun subscribeToStream(sub: StreamHandler<T>): Unsubscribe {
        // An entry of this subscription's own (see ownEntry)
        val entry = ownEntry(sub)

        this.subscriptions.add(entry)

        try {
            onSub()
        } catch (e: Throwable) {
            // No unsubscribe reaches the caller: the handler must not stay behind
            subscriptions.remove(entry)
            throw e
        }

        try {
            sub(current)
        } catch (e: Throwable) {
            // The handler threw on its first value: nor here, and what onSub started is released with it
            subscriptions.remove(entry)
            onUnSub()
            throw e
        }

        return {
            subscriptions.remove(entry)
            onUnSub()
        }
    }

    /**
     * @see StreamSource.removeAllSubscriptions
     */
    override fun removeAllSubscriptions() {
        this.subscriptions.clear()
    }

    /**
     * Hook called when a subscription was added
     */
    protected open fun onSub() {}

    /**
     * Hook called when a subscription was unsubscribed
     */
    protected open fun onUnSub() {}
}
