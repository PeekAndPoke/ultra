package de.peekandpoke.mutator

fun <V> Set<V>.mutator(child: V.() -> Mutator<V>): SetMutator<V> = SetMutatorImpl(this, child)

fun <V> SetMutator<V>.add(element: V) = this@add.add(getChildMutator(element))

/**
 * Returns a set containing only the mutators whose underlying value is an instance of specified type parameter [X].
 */
inline fun <reified X : V, V> SetMutator<V>.filterMutatorsOf(): Set<Mutator<X>> {
    return mapNotNull { mutator ->
        if (mutator.get() is X) {
            @Suppress("UNCHECKED_CAST")
            mutator as Mutator<X>
        } else {
            null
        }
    }.toSet()
}

interface SetMutator<V> : Mutator<MutableSet<V>>, MutableSet<Mutator<V>> {
    fun getChildMutator(child: V): Mutator<V>
}

class SetMutatorImpl<V>(initial: Set<V>, private val childToMutator: V.() -> Mutator<V>) :
    Mutator.Base<MutableSet<V>>(initial.toMutableSet()),
    SetMutator<V> {

    /**
     * Iterator impl
     */
    private inner class It : MutableIterator<Mutator<V>> {

        // We make a copy of the value, so we can modify while iteration
        private val inner = doGet().toList().iterator()
        private var current: V? = null

        override fun hasNext() = inner.hasNext()

        override fun next(): Mutator<V> {

            return inner.next().run {
                // remember the current element, so we can use it for remove()
                current = this

                // Track the most recently replaced version of this element
                var currentTrackedValue = this

                childToMutator(this).onChange { newValue ->
                    // Replace the currently known value with the new one
                    replace(currentTrackedValue, newValue)
                    // Update our tracker for the next modification
                    currentTrackedValue = newValue
                    current = newValue
                }
            }
        }

        override fun remove() {
            current?.let {
                doGet().remove(it)
                notifyObservers() // Don't forget to notify!
            }
        }

        /**
         * Helper to work around compiler issues with casting to MutableSet<V>
         */
        @Suppress("USELESS_CAST", "NOTHING_TO_INLINE")
        private inline fun doGet(): MutableSet<V> = get() as MutableSet<V>
    }

    override fun getChildMutator(child: V): Mutator<V> = childToMutator(child)

    /**
     * Returns the size of the list
     */
    override val size get() = get().size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = get().isEmpty()

    /**
     * Returns true if the set contains the given [element]
     */
    override fun contains(element: Mutator<V>) = get().contains(element.get())

    /**
     * Returns true if the set contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<V>>) = get().containsAll(elements.extract())

    /**
     * Clears the whole list
     */
    override fun clear() {
        if (get().isNotEmpty()) {
            get().clear()
            notifyObservers()
        }
    }

    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    override fun add(element: Mutator<V>): Boolean {
        return get().add(element.get()).also { if (it) notifyObservers() }
    }

    /**
     * Adds all [elements] to the set
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return get().addAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Removes an [element] from the set
     *
     * @return 'true' of the element was removed from the set
     */
    override fun remove(element: Mutator<V>): Boolean {
        return get().remove(element.get()).also { if (it) notifyObservers() }
    }

    /**
     * Removes the specified [elements] from the list
     *
     * @return 'true' when the list has been modified
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        return get().removeAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Retains all of the given [elements]
     *
     * @return 'true' when the list has been modified
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        return get().retainAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Gets an iterator for the set
     */
    override fun iterator(): MutableIterator<Mutator<V>> {
        return It()
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun replace(old: V, new: V) {
        val didRemove = get().remove(old)
        if (didRemove) {
            get().add(new)
            notifyObservers()
        }
    }

    /**
     * Extracts the values of the given mutators
     */
    private fun Collection<Mutator<V>>.extract(): Set<V> = map { it.get() }.toSet()
}
