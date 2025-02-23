package de.peekandpoke.ultra.common

typealias OnChange<T> = (T) -> Unit
typealias Unsubscribe = () -> Unit

interface Observable<T> {

    private class Subscription<T, O>(
        val id: Int,
        val unsubscribe: WeakReference<O>,
        val subscription: OnChange<T>,
    )

    class Subscriptions<T> : Observable<T> {

        /** Sync lock */
        private val lock = Any()

        /** Id counter for subscriptions */
        private var idCounter = 0

        /** The subscriptions */
        private val subscriptions: MutableSet<Subscription<T, *>> = mutableSetOf()

        /**
         * Subscribes to the observable stream and executes the given block for each emitted item.
         */
        override fun <O> observe(observer: O, block: OnChange<T>): Unsubscribe {
            val id = RunSync(lock) {
                idCounter++
            }

            val subscription = Subscription(
                id = id,
                unsubscribe = WeakReference(observer),
                subscription = block,
            )

            subscriptions.add(subscription)

            val unsubscribe: Unsubscribe = { unsubscribeId(id) }

            return unsubscribe
        }

        /**
         * Emit a new value to all observers
         */
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

    /**
     * Subscribes to the observable for the given [observer] and the callback [block].
     */
    fun <O> observe(observer: O, block: OnChange<T>): Unsubscribe
}

interface Observer {
    fun <T> Observable<T>.observe(block: OnChange<T>) = observe(this, block)
}

fun <O : Any, T> O.observe(observable: Observable<T>, block: OnChange<T>) = observable.observe(this, block)
