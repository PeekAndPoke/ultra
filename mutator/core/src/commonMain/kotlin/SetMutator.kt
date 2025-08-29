package de.peekandpoke.mutator

fun <V> Set<V>.mutator(child: V.() -> Mutator<V>): SetMutator<V> = SetMutatorImpl(this, child)

fun <V> SetMutator<V>.add(element: V) = add(getChildMutator(element))

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
        private val inner = get().toList().iterator()
        private var current: V? = null

        override fun hasNext() = inner.hasNext()

        override fun next(): Mutator<V> {

            return inner.next().run {
                // remember the current element, so we can use it for remove()
                current = this
                // Keep track if the last state of our element, so we can remove and add correctly
                val last = this

                childToMutator(this).onChange {
                    replace(last, it)
                }
            }
        }

        override fun remove() {
            current?.let {
                get().remove(it)
            }
        }
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
        modifyValue { it.also { clear() } }.notifyObservers()
    }

    /**
     * Adds the specified element to the set.
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    override fun add(element: Mutator<V>): Boolean {
        return get().add(element.get()).notifyObservers()
    }

    /**
     * Adds all [elements] to the set
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return get().addAll(elements.extract()).notifyObservers()
    }

    /**
     * Removes an [element] from the set
     *
     * @return 'true' of the element was removed from the set
     */
    override fun remove(element: Mutator<V>): Boolean {
        return get().remove(element.get()).notifyObservers()
    }

    /**
     * Removes the specified [elements] from the list
     *
     * @return 'true' when the list has been modified
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        return get().removeAll(elements.extract()).notifyObservers()
    }

    /**
     * Retains all of the given [elements]
     *
     * @return 'true' when the list has been modified
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        return get().retainAll(elements.extract()).notifyObservers()
    }

    /**
     * Gets an iterator for the set
     */
    override fun iterator(): MutableIterator<Mutator<V>> {
        return It()
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun replace(old: V, new: V) {
        get().remove(old)
        get().add(new)
        notifyObservers()
    }

    /**
     * Extracts the values of the given mutators
     */
    private fun Collection<Mutator<V>>.extract(): Set<V> = map { it.get() }.toSet()
}
