package de.peekandpoke.ultra.common

typealias OnChange<T> = (T) -> Unit
typealias Unsubscribe = () -> Unit

interface Observable<T> {

    private class Subscription<T>(
        val id: Int,
        val unsubscribe: WeakReference<Unsubscribe>,
        val subscription: OnChange<T>,
    )

    class Subscriptions<T> : Observable<T> {

        /** Sync lock */
        private val lock = Any()

        /** Id counter for subscriptions */
        private var idCounter = 0

        /** The subscriptions */
        private val subscriptions: MutableSet<Subscription<T>> = mutableSetOf()

        /**
         * Subscribes to the observable stream and executes the given block for each emitted item.
         */
        override fun subscribe(block: OnChange<T>): Unsubscribe {
            val id = RunSync(lock) {
                idCounter++
            }

            val unsubscribe: Unsubscribe = { unsubscribeId(id) }

            val subscription = Subscription(
                id = id,
                unsubscribe = WeakReference(unsubscribe),
                subscription = block,
            )

            subscriptions.add(subscription)

            return unsubscribe
        }

        fun emit(value: T) {
            cleanUp()
            subscriptions.forEach { it.subscription(value) }
        }

        /**
         * Clears all subscriptions and removes them from the subscriptions list.
         */
        fun unsubscribeAll() {
            subscriptions.forEach { it.unsubscribe.clear() }
            subscriptions.clear()
        }

        /**
         * Determines whether there are any active subscriptions.
         */
        fun hasSubscriptions(): Boolean {
            cleanUp()
            return subscriptions.isNotEmpty()
        }

        /**
         * Returns the number of subscriptions
         */
        fun numSubscriptions(): Int {
            cleanUp()
            return subscriptions.size
        }

        /**
         * Removes a subscription by its id
         */
        private fun unsubscribeId(id: Int) {
            cleanUp()
            subscriptions.removeAll { it.id == id }
        }

        /**
         * Cleans up the subscriptions by removing any weak references that have been garbage collected.
         * This method removes any subscriptions from the subscriptions list where the referenced object is null.
         */
        private fun cleanUp() {
            subscriptions.removeAll { it.unsubscribe.get() == null }
        }
    }

    fun subscribe(block: OnChange<T>): Unsubscribe
}
