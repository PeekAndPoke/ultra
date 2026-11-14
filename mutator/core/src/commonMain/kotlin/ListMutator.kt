package io.peekandpoke.mutator

fun <V> List<V>.mutator(child: V.() -> Mutator<V>): ListMutator<V> =
    ListMutatorImpl(this, child)

fun <V> ListMutator<V>.add(element: V): Boolean = add(getChildMutator(element))

fun <V> ListMutator<V>.add(index: Int, element: V): Unit = add(index, getChildMutator(element))

/**
 * A [Mutator] for a list that is also a [MutableList] of mutators for its elements.
 *
 * An element mutator from [get], an iterator or [subList] writes back to its own row, wherever later structural
 * changes move that row. Its write is not applied when the row is gone (removed, cleared, the whole value replaced or
 * reset) or holds another instance than the one this mutator last saw: something else wrote it (another element
 * mutator, `set`, a reorder such as `sort`, `reverse` or `shuffle`, or a move such as `add(j, removeAt(i))`).
 * Such a write is dropped silently, but the element mutator keeps its value, so a later write that does land
 * carries it. Following an element across a reorder is a design question (ULTRA-00323). The check is identity:
 * for strings, numbers and booleans an equal value written by someone else may or may not count as the same
 * instance, depending on platform and interning.
 *
 * Row tracking belongs to this mutator instance. Change the list through it: the value from [get] is for reading,
 * and the list passed to `set(list)` is kept by reference. `remove(element)`, `indexOf` and `contains` compare by
 * value and take the first equal row; for a specific row use `removeAt` or an iterator.
 */
interface ListMutator<V> : Mutator<MutableList<V>>, MutableList<Mutator<V>> {
    fun getChildMutator(child: V): Mutator<V>

    /**
     * Returns a read-only snapshot of the child mutators from [fromIndex] (inclusive) to [toIndex] (exclusive).
     *
     * Unlike `List.subList` it is not a view: later changes of this list do not show in it. A change through one of
     * its elements writes back to its row in this list, as for [get]. Structural changes to the snapshot throw
     * [UnsupportedOperationException], so `subList(a, b).clear()` throws: to remove or reorder a range, use this list
     * (`removeAt`, `removeAll` / `retainAll`, `listIterator(from)`).
     *
     * @throws IndexOutOfBoundsException if [fromIndex] < 0 or [toIndex] > size
     * @throws IllegalArgumentException if [fromIndex] > [toIndex]
     */
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Mutator<V>>
}

class ListMutatorImpl<V>(value: List<V>, private val childToMutator: V.() -> Mutator<V>) :
    Mutator.Base<MutableList<V>>(value.toMutableList()),
    ListMutator<V> {

    /** Marks one row; element mutators find their row by it. */
    private class RowToken

    /** One token per row, kept in step with every structural change made through this mutator. */
    private val tokens: MutableList<RowToken> = MutableList(get().size) { RowToken() }

    override fun snapshotInitial(value: MutableList<V>): MutableList<V> = value.toMutableList()

    /** Replacing the whole value gives every row a new token, so element mutators held before drop their writes. */
    override fun modifyValue(block: (MutableList<V>) -> MutableList<V>) {
        super.modifyValue { current -> block(current).also { renewTokens(it.size) } }
    }

    override fun reset(): MutableList<V> {
        renewTokens(getInitialValue().size)
        return super<Mutator.Base>.reset()
    }

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
            if (!hasNext()) throw NoSuchElementException()
            val idx = pos

            return trackedChild(idx).also {
                pos = idx + 1
                lastReturned = idx
            }
        }

        override fun hasPrevious(): Boolean = pos > 0

        override fun previousIndex(): Int = pos - 1

        override fun previous(): Mutator<V> {
            if (!hasPrevious()) throw NoSuchElementException()
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
            rows().clear()
            notifyObservers()
        }
    }

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: Mutator<V>): Boolean {
        val rows = rows()
        get().add(element.get())
        rows.add(RowToken())
        notifyObservers()
        return true
    }

    /**
     * Inserts an element into the list at the specified [index].
     */
    override fun add(index: Int, element: Mutator<V>) {
        val rows = rows()
        get().add(index, element.get())
        rows.add(index, RowToken())
        notifyObservers()
    }

    /**
     * Adds all the elements of the specified collection to the end of this list.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return addAll(size, elements)
    }

    /**
     * Inserts all the elements of the specified collection [elements] into this list at the specified [index].
     */
    override fun addAll(index: Int, elements: Collection<Mutator<V>>): Boolean {
        // Read the values once, before the insert: elements may be this list itself or a view of it
        val values = elements.extract()
        val rows = rows()
        return get().addAll(index, values).also {
            if (it) {
                rows.addAll(index, List(values.size) { RowToken() })
                notifyObservers()
            }
        }
    }

    /**
     * Removes the given element
     *
     * @return true when the element has been removed from the list
     */
    override fun remove(element: Mutator<V>): Boolean {
        val index = get().indexOf(element.get())
        if (index == -1) return false

        removeAt(index)
        return true
    }

    /**
     * Removes an element at the specified [index] from the list.
     *
     * NOTICE: modifying the returned element will have no effect
     *
     * @return the element that has been removed.
     */
    override fun removeAt(index: Int): Mutator<V> {
        val rows = rows()
        val removed = get().removeAt(index)
        rows.removeAt(index)
        notifyObservers()
        return removed.childToMutator()
    }

    /**
     * Removes all of the given [elements]
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        val values = elements.extract()
        return removeRowsWhere { it in values }
    }

    /**
     * Retains all of the given [elements]
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        val values = elements.extract()
        return removeRowsWhere { it !in values }
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
        if (index < 0 || index > size) throw IndexOutOfBoundsException("index: $index, size: $size")

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
     * It finds its row by the row's token. A write lands only if the row still holds the very instance this mutator
     * last saw (identity, not equality: an equal value written by someone else still counts as someone else's).
     * Two NaN count as the same instance: on JS `NaN === NaN` is false.
     */
    private fun trackedChild(index: Int): Mutator<V> {
        var seen = get()[index]
        val token = rows()[index]
        var hint = index

        return seen.childToMutator().onChange { new ->
            val rows = rows()
            val pos = if (rows.getOrNull(hint) === token) hint else rows.indexOf(token)

            if (pos != -1 && sameInstance(get()[pos], seen)) {
                // Update before notifying, so a write from inside an observer sees this one
                seen = new
                hint = pos
                setAt(pos, new)
            }
        }
    }

    /** The row tokens; new ones for every row when the list was changed in place, bypassing this mutator. */
    private fun rows(): MutableList<RowToken> {
        if (tokens.size != get().size) renewTokens(get().size)
        return tokens
    }

    private fun renewTokens(size: Int) {
        tokens.clear()
        repeat(size) { tokens.add(RowToken()) }
    }

    /** Identity, except that two NaN of the same type count as the same instance. */
    private fun sameInstance(a: Any?, b: Any?): Boolean = a === b ||
        (a is Double && b is Double && a.isNaN() && b.isNaN()) ||
        (a is Float && b is Float && a.isNaN() && b.isNaN())

    /** Removes the rows whose value matches [predicate], with their tokens. */
    private fun removeRowsWhere(predicate: (V) -> Boolean): Boolean {
        val rows = rows()
        val list = get()
        var removed = false

        for (i in list.indices.reversed()) {
            if (predicate(list[i])) {
                list.removeAt(i)
                rows.removeAt(i)
                removed = true
            }
        }

        if (removed) notifyObservers()
        return removed
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
 * platform. `contains`, `containsAll`, `indexOf` and `lastIndexOf` compare by value, as [ListMutatorImpl] does;
 * `equals` and `hashCode` compare the element mutators.
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
