package io.peekandpoke.mutator

fun <V> List<V>.mutator(child: V.() -> Mutator<V>): ListMutator<V> =
    ListMutatorImpl(this, child)

fun <V> ListMutator<V>.add(element: V): Boolean = add(getChildMutator(element))

fun <V> ListMutator<V>.add(index: Int, element: V) = add(index, getChildMutator(element))

interface ListMutator<V> : Mutator<MutableList<V>>, MutableList<Mutator<V>> {
    fun getChildMutator(child: V): Mutator<V>
}

class ListMutatorImpl<V>(value: List<V>, private val childToMutator: V.() -> Mutator<V>) :
    Mutator.Base<MutableList<V>>(value.toMutableList()),
    ListMutator<V> {

    override fun snapshotInitial(value: MutableList<V>): MutableList<V> = value.toMutableList()

    /**
     * Iterator impl
     */
    internal inner class It(startIndex: Int = 0) : MutableListIterator<Mutator<V>> {

        private var pos = startIndex
        private var current: Mutator<V>? = null

        override fun nextIndex(): Int = pos

        override fun hasNext(): Boolean = pos < doGet().size

        override fun next(): Mutator<V> {
            val idx = pos++

            // We remember the original object, just like in get(index)
            val initial = doGet()[idx]

            return initial.run {
                // return the current element mapped to a mutator with onModify callback
                childToMutator(this)
                    .onChange { new ->
                        // Dynamically find the element because the list might have changed
                        val currentPos = doGet().indexOfFirst { it === initial }
                        if (currentPos != -1) {
                            setAt(currentPos, new)
                        }
                    }
                    .also {
                        // remember the current element, so we can use it for remove()
                        current = it
                    }
            }
        }

        override fun hasPrevious(): Boolean = pos > 0

        override fun previousIndex(): Int = pos - 1

        override fun previous(): Mutator<V> {
            val idx = --pos
            val initial = doGet()[idx]

            return initial.run {
                childToMutator(this)
                    .onChange { new ->
                        val currentPos = doGet().indexOfFirst { it === initial }
                        if (currentPos != -1) {
                            setAt(currentPos, new)
                        }
                    }
                    .also {
                        current = it
                    }
            }
        }

        override fun remove() {
            removeAt(--pos)
        }

        override fun add(element: Mutator<V>) {
            add(pos++, element)
        }

        override fun set(element: Mutator<V>) {
            setAt(pos - 1, element.get())
        }

        /**
         * Helper to work around compiler issues with casting to MutableList<V>
         */
        private fun doGet(): MutableList<V> = this@ListMutatorImpl.get()
    }

    override fun getChildMutator(child: V): Mutator<V> = childToMutator(child)

    /**
     * The size of the currently mutated list.
     */
    override val size: Int get() = get().size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = get().isEmpty()

    /**
     * Returns true if the list contains the given [element]
     */
    override fun contains(element: Mutator<V>) = get().contains(element.get())

    /**
     * Returns true if the list contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<V>>) = get().containsAll(elements.extract())

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun indexOf(element: Mutator<V>) = get().indexOf(element.get())

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun lastIndexOf(element: Mutator<V>) = get().lastIndexOf(element.get())

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
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: Mutator<V>): Boolean {
        return get().add(element.get()).also { if (it) notifyObservers() }
    }

    /**
     * Inserts an element into the list at the specified [index].
     */
    override fun add(index: Int, element: Mutator<V>) {
        get().add(index, element.get())
        notifyObservers()
    }

    /**
     * Adds all the elements of the specified collection to the end of this list.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return get().addAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Inserts all the elements of the specified collection [elements] into this list at the specified [index].
     */
    override fun addAll(index: Int, elements: Collection<Mutator<V>>): Boolean {
        return get().addAll(index, elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Removes the given element
     *
     * @return true when the element has been removed from the list
     */
    override fun remove(element: Mutator<V>): Boolean {
        return get().remove(element.get()).also { if (it) notifyObservers() }
    }

    /**
     * Removes an element at the specified [index] from the list.
     *
     * NOTICE: modifying the returned element will have no effect
     *
     * @return the element that has been removed.
     */
    override fun removeAt(index: Int): Mutator<V> {
        return get().removeAt(index).childToMutator().also { notifyObservers() }
    }

    /**
     * Removes all of the given [elements]
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        return get().removeAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Retains all of the given [elements]
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        return get().retainAll(elements.extract()).also { if (it) notifyObservers() }
    }

    /**
     * Get the value at the given index
     */
    override fun get(index: Int): Mutator<V> {
        // We remember the original object
        val initial = get()[index]

        return initial.childToMutator().onChange { new ->
            // Is our element still in the list and where is it?
            // Notice: We need to use the object equality check here '==='
            val pos = get().indexOfFirst { it === initial }

            if (pos != -1) {
                setAt(pos, new)
            }
        }
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * Returns: the element previously at the specified position.
     */
    override fun set(index: Int, element: Mutator<V>): Mutator<V> {
        val old = get()[index]
        val new = element.get()

        if (old != new) {
            get()[index] = new
            notifyObservers()
        }

        return old.childToMutator()
    }

    /**
     * Returns an iterator
     */
    override fun iterator(): MutableIterator<Mutator<V>> {
        return It()
    }

    /**
     * Returns a list iterator
     */
    override fun listIterator(): MutableListIterator<Mutator<V>> {
        return It()
    }

    /**
     * Returns a list iterator starting at the specified [index].
     */
    override fun listIterator(index: Int): MutableListIterator<Mutator<V>> {
        return It(index)
    }

    /**
     * Returns a read-only snapshot of the child mutators from [fromIndex] (inclusive) to [toIndex] (exclusive).
     *
     * Each element is a child mutator as [get] returns it: changing it writes back to this list and notifies its
     * observers. The element is found by identity, so if the same instance is in the list twice, the change goes to
     * its first position. The snapshot is not a view: later structural changes to this list do not show in it.
     * Every structural change to the snapshot or its iterators throws [UnsupportedOperationException].
     *
     * @throws IndexOutOfBoundsException if [fromIndex] < 0 or [toIndex] > size
     * @throws IllegalArgumentException if [fromIndex] > [toIndex]
     */
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutator<V>> {
        val size = this.size

        if (fromIndex < 0 || toIndex > size) {
            throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: $size")
        }
        if (fromIndex > toIndex) {
            throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
        }

        return ReadOnlySnapshotList((fromIndex until toIndex).map { get(it) })
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Extracts the values of the given mutators
     */
    private fun Collection<Mutator<V>>.extract(): List<V> = map { it.get() }

    /**
     * Sets the [element] at the given [index]
     */
    private fun setAt(index: Int, element: V) {
        get()[index] = element
        notifyObservers()
    }
}

/**
 * A [MutableList] that refuses every structural change, the same on every platform. Returned by
 * [ListMutatorImpl.subList].
 */
private class ReadOnlySnapshotList<E>(private val elements: List<E>) : MutableList<E> {

    private class It<E>(private val inner: ListIterator<E>) : MutableListIterator<E> {
        override fun hasNext(): Boolean = inner.hasNext()
        override fun next(): E = inner.next()
        override fun hasPrevious(): Boolean = inner.hasPrevious()
        override fun previous(): E = inner.previous()
        override fun nextIndex(): Int = inner.nextIndex()
        override fun previousIndex(): Int = inner.previousIndex()
        override fun add(element: E) = refuse()
        override fun remove() = refuse()
        override fun set(element: E) = refuse()
    }

    override val size: Int get() = elements.size
    override fun isEmpty(): Boolean = elements.isEmpty()
    override fun contains(element: E): Boolean = elements.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = this.elements.containsAll(elements)
    override fun get(index: Int): E = elements[index]
    override fun indexOf(element: E): Int = elements.indexOf(element)
    override fun lastIndexOf(element: E): Int = elements.lastIndexOf(element)

    override fun iterator(): MutableIterator<E> = It(elements.listIterator())
    override fun listIterator(): MutableListIterator<E> = It(elements.listIterator())
    override fun listIterator(index: Int): MutableListIterator<E> = It(elements.listIterator(index))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> =
        ReadOnlySnapshotList(elements.subList(fromIndex, toIndex).toList())

    override fun add(element: E): Boolean = refuse()
    override fun add(index: Int, element: E) = refuse()
    override fun addAll(elements: Collection<E>): Boolean = refuse()
    override fun addAll(index: Int, elements: Collection<E>): Boolean = refuse()
    override fun remove(element: E): Boolean = refuse()
    override fun removeAt(index: Int): E = refuse()
    override fun removeAll(elements: Collection<E>): Boolean = refuse()
    override fun retainAll(elements: Collection<E>): Boolean = refuse()
    override fun set(index: Int, element: E): E = refuse()
    override fun clear() = refuse()

    override fun equals(other: Any?): Boolean = other === this || elements == other
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String = elements.toString()
}

private fun refuse(): Nothing = throw UnsupportedOperationException("A ListMutator.subList() snapshot is read-only")
