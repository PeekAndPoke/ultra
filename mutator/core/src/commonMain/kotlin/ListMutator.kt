package io.peekandpoke.mutator

fun <V> List<V>.mutator(child: V.() -> Mutator<V>): ListMutator<V> =
    ListMutatorImpl(this, child)

fun <V> ListMutator<V>.add(element: V): Boolean = add(getChildMutator(element))

fun <V> ListMutator<V>.add(index: Int, element: V): Unit = add(index, getChildMutator(element))

interface ListMutator<V> : Mutator<MutableList<V>>, MutableList<Mutator<V>> {
    fun getChildMutator(child: V): Mutator<V>

    /**
     * Returns a read-only snapshot of the child mutators from [fromIndex] (inclusive) to [toIndex] (exclusive).
     *
     * Unlike `List.subList` it is not a view: later changes of this list do not show in it. A change through one of
     * its elements writes back to this list and notifies its observers; it is dropped if this list has meanwhile
     * replaced or removed that element. Structural changes to the snapshot throw [UnsupportedOperationException], so
     * `subList(a, b).clear()` throws: to remove or reorder a range, use this list (`removeAt`, `removeAll` /
     * `retainAll`, `listIterator(from)`).
     *
     * @throws IndexOutOfBoundsException if [fromIndex] < 0 or [toIndex] > size
     * @throws IllegalArgumentException if [fromIndex] > [toIndex]
     */
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutator<V>>
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

        /** Index of the element last returned by [next] or [previous]; -1 when there is none to [remove] or [set]. */
        private var lastReturned = -1

        override fun nextIndex(): Int = pos

        override fun hasNext(): Boolean = pos < size

        override fun next(): Mutator<V> {
            val idx = pos

            return trackedChild(idx).also {
                pos = idx + 1
                lastReturned = idx
            }
        }

        override fun hasPrevious(): Boolean = pos > 0

        override fun previousIndex(): Int = pos - 1

        override fun previous(): Mutator<V> {
            val idx = pos - 1

            return trackedChild(idx).also {
                pos = idx
                lastReturned = idx
            }
        }

        override fun remove() {
            check(lastReturned != -1) { "remove() needs a preceding next() or previous()" }

            removeAt(lastReturned)
            pos = lastReturned
            lastReturned = -1
        }

        override fun add(element: Mutator<V>) {
            add(pos++, element)
            lastReturned = -1
        }

        override fun set(element: Mutator<V>) {
            check(lastReturned != -1) { "set() needs a preceding next() or previous()" }

            setAt(lastReturned, element.get())
        }
    }

    override fun getChildMutator(child: V): Mutator<V> = childToMutator(child)

    /**
     * The size of the currently mutated list.
     */
    override val size: Int get() = get().size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty(): Boolean = get().isEmpty()

    /**
     * Returns true if the list contains the given [element]
     */
    override fun contains(element: Mutator<V>): Boolean = get().contains(element.get())

    /**
     * Returns true if the list contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<V>>): Boolean = get().containsAll(elements.extract())

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun indexOf(element: Mutator<V>): Int = get().indexOf(element.get())

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun lastIndexOf(element: Mutator<V>): Int = get().lastIndexOf(element.get())

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
    override fun get(index: Int): Mutator<V> = trackedChild(index)

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
     * The child mutator for the element at [index], as [get], the iterators and [subList] hand it out.
     *
     * Each change writes back to the element this mutator last wrote: at its last index if it is still there, else
     * at the first position holding that instance. The change is dropped when the list no longer holds it.
     */
    private fun trackedChild(index: Int): Mutator<V> {
        var tracked = get()[index]
        var hint = index

        return tracked.childToMutator().onChange { new ->
            // Notice: identity '===', not equality, so equal elements elsewhere in the list are never hit
            val list = get()
            val pos = if (list.getOrNull(hint) === tracked) hint else list.indexOfFirst { it === tracked }

            if (pos != -1) {
                setAt(pos, new)
                tracked = new
                hint = pos
            }
        }
    }

    /**
     * Sets the [element] at the given [index]
     */
    private fun setAt(index: Int, element: V) {
        get()[index] = element
        notifyObservers()
    }
}

/**
 * The read-only snapshot [ListMutator.subList] returns: it refuses every structural change, the same on every
 * platform, and compares elements by value, as [ListMutatorImpl] does.
 */
private class ReadOnlySnapshotList<V>(private val elements: List<Mutator<V>>) : MutableList<Mutator<V>> {

    private class It<V>(private val inner: ListIterator<Mutator<V>>) : MutableListIterator<Mutator<V>> {
        override fun hasNext(): Boolean = inner.hasNext()
        override fun next(): Mutator<V> = inner.next()
        override fun hasPrevious(): Boolean = inner.hasPrevious()
        override fun previous(): Mutator<V> = inner.previous()
        override fun nextIndex(): Int = inner.nextIndex()
        override fun previousIndex(): Int = inner.previousIndex()
        override fun add(element: Mutator<V>): Unit = refuse()
        override fun remove(): Unit = refuse()
        override fun set(element: Mutator<V>): Unit = refuse()
    }

    private fun values(): List<V> = elements.map { it.get() }

    override val size: Int get() = elements.size
    override fun isEmpty(): Boolean = elements.isEmpty()
    override fun contains(element: Mutator<V>): Boolean = values().contains(element.get())
    override fun containsAll(elements: Collection<Mutator<V>>): Boolean =
        values().containsAll(elements.map { it.get() })

    override fun get(index: Int): Mutator<V> = elements[index]
    override fun indexOf(element: Mutator<V>): Int = values().indexOf(element.get())
    override fun lastIndexOf(element: Mutator<V>): Int = values().lastIndexOf(element.get())

    override fun iterator(): MutableIterator<Mutator<V>> = It(elements.listIterator())
    override fun listIterator(): MutableListIterator<Mutator<V>> = It(elements.listIterator())
    override fun listIterator(index: Int): MutableListIterator<Mutator<V>> = It(elements.listIterator(index))

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutator<V>> =
        ReadOnlySnapshotList(elements.subList(fromIndex, toIndex).toList())

    override fun add(element: Mutator<V>): Boolean = refuse()
    override fun add(index: Int, element: Mutator<V>): Unit = refuse()
    override fun addAll(elements: Collection<Mutator<V>>): Boolean = refuse()
    override fun addAll(index: Int, elements: Collection<Mutator<V>>): Boolean = refuse()
    override fun remove(element: Mutator<V>): Boolean = refuse()
    override fun removeAt(index: Int): Mutator<V> = refuse()
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean = refuse()
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean = refuse()
    override fun set(index: Int, element: Mutator<V>): Mutator<V> = refuse()
    override fun clear(): Unit = refuse()

    override fun equals(other: Any?): Boolean = other === this || elements == other
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String = values().toString()
}

private fun refuse(): Nothing = throw UnsupportedOperationException("A ListMutator.subList() snapshot is read-only")
