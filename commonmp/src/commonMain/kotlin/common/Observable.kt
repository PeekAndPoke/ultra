package de.peekandpoke.ultra.common

typealias OnChange<V> = (V) -> Unit
typealias Subscription<T> = (T) -> Unit
typealias Unsubscribe = () -> Unit

interface Observable<T> {

    class Subscriptions<T> : Observable<T> {

        /**
         * The subscription the stream source has.
         */
        private val subscriptions: MutableSet<WeakReference<Subscription<T>>> = mutableSetOf()

        /**
         * Subscribes to the observable stream and executes the given block for each emitted item.
         */
        override fun subscribe(block: (T) -> Unit): Unsubscribe {
            val ref = WeakReference(block)

            subscriptions.add(ref)

            return { subscriptions.remove(ref) }
        }

        fun emit(value: T) {
            cleanUp()
            subscriptions.forEach { it.get()?.invoke(value) }
        }

        /**
         * Clears all subscriptions and removes them from the subscriptions list.
         */
        fun unsubscribeAll() {
            subscriptions.forEach { it.clear() }
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
         * Cleans up the subscriptions by removing any weak references that have been garbage collected.
         * This method removes any subscriptions from the subscriptions list where the referenced object is null.
         */
        private fun cleanUp() {
            subscriptions.removeAll { it.get() == null }
        }
    }

    fun subscribe(block: (T) -> Unit): Unsubscribe
}
