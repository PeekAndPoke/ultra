package io.peekandpoke.ultra.common

/** Callback type for change notifications. */
typealias OnChange<T> = (T) -> Unit

/** A function that, when invoked, cancels a subscription. */
typealias Unsubscribe = () -> Unit

/**
 * An observable that allows observers to subscribe to value changes.
 *
 * A subscription lives until it is cancelled through the [Unsubscribe] returned by [observe], or
 * until the observable itself is collected. The callback — and everything it captures — is held
 * strongly, so a subscriber that outlives its observable has to unsubscribe.
 *
 * @param T The type of value being observed.
 */
interface Observable<T> {

    /** Default implementation of [Observable], managing a set of subscriptions. */
    class Subscriptions<T> : Observable<T> {

        /** A single subscription: the callback plus the id used to cancel it again. */
        private class Subscription<T>(
            val id: Int,
            val onChange: OnChange<T>,
        )

        /** Sync lock */
        private val lock = Any()

        /** Id counter for subscriptions */
        private var idCounter = 0

        /** The subscriptions */
        private val subscriptions: MutableSet<Subscription<T>> = mutableSetOf()

        /**
         * Subscribes to the observable stream and executes the given block for each emitted item.
         *
         * @return An [Unsubscribe] function that removes this subscription.
         */
        override fun observe(block: OnChange<T>): Unsubscribe {
            val id = RunSync(lock) {
                idCounter++
            }

            subscriptions.add(Subscription(id = id, onChange = block))

            return { unsubscribeId(id) }
        }

        /**
         * Emit a new value to all observers, in subscription order.
         */
        fun emit(value: T) {
            // Iterate a snapshot: a callback is free to subscribe or unsubscribe, which would
            // otherwise mutate the set while it is being walked. A subscription added during the
            // round is not notified in it, and one removed during the round no longer fires.
            subscriptions.toList().forEach {
                if (it in subscriptions) {
                    it.onChange(value)
                }
            }
        }

        /**
         * Cancels every subscription.
         */
        fun unsubscribeAll() {
            subscriptions.clear()
        }

        /**
         * Determines whether there are any active subscriptions.
         */
        fun hasSubscriptions(): Boolean = subscriptions.isNotEmpty()

        /**
         * Returns the number of subscriptions
         */
        fun numSubscriptions(): Int = subscriptions.size

        /**
         * Removes a subscription by its id
         */
        private fun unsubscribeId(id: Int) {
            subscriptions.removeAll { it.id == id }
        }
    }

    /**
     * Subscribes [block] to this observable.
     *
     * @return An [Unsubscribe] function that cancels the subscription when invoked.
     */
    fun observe(block: OnChange<T>): Unsubscribe
}
