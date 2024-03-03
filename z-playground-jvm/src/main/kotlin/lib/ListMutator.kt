package de.peekandpoke.ultra.playground.lib

import de.peekandpoke.ultra.common.OnChange

fun <V> List<V>.mutator(child: V.(OnChange<V>) -> Mutator<V>): ListMutator<V> = ListMutator(this, child)

fun <V> ListMutator<V>.add(element: V): Boolean = add(Mutator.Null(element))

fun <V> ListMutator<V>.add(index: Int, element: V) = add(index, Mutator.Null(element))

class ListMutator<V>(value: List<V>, private val child: V.(OnChange<V>) -> Mutator<V>) :
    Mutator.Base<MutableList<V>>(value.toMutableList()),
    MutableList<Mutator<V>> {

    /**
     * Iterator impl
     */
    internal inner class It(startIndex: Int = 0) : MutableListIterator<Mutator<V>> {

        private var pos = startIndex
        private var current: Mutator<V>? = null

        override fun nextIndex(): Int = pos

        override fun hasNext(): Boolean = pos < value.size

        override fun next(): Mutator<V> {
            val idx = pos++

            return value[idx].run {
                // return the current element mapped to a mutator with onModify callback
                child(this) { setAt(idx, it) }.also {
                    // remember the current element, so we can use it for remove()
                    current = it
                }
            }
        }

        override fun hasPrevious(): Boolean = pos > 0

        override fun previousIndex(): Int = pos - 1

        override fun previous(): Mutator<V> {
            TODO("Not yet implemented")
        }

        override fun remove() {
            TODO("Not yet implemented")
        }

        override fun add(element: Mutator<V>) {
            TODO("Not yet implemented")
        }

        override fun set(element: Mutator<V>) {
            TODO("Not yet implemented")
        }
    }

    /**
     * The size of the currently mutated list.
     */
    override val size: Int get() = value.size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = value.isEmpty()

    /**
     * Returns true if the list contains the given [element]
     */
    override fun contains(element: Mutator<V>) = value.contains(element.value)

    /**
     * Returns true if the list contains all of the given [elements]
     */
    override fun containsAll(elements: Collection<Mutator<V>>) = value.containsAll(elements.extract())

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun indexOf(element: Mutator<V>) = value.indexOf(element.value)

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun lastIndexOf(element: Mutator<V>) = value.lastIndexOf(element.value)

    /**
     * Clears the whole list
     */
    override fun clear() {
        modify { it.also { clear() } }.commit()
    }

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: Mutator<V>): Boolean {
        return value.add(element.value).commit()
    }

    /**
     * Inserts an element into the list at the specified [index].
     */
    override fun add(index: Int, element: Mutator<V>) {
        return value.add(index, element.value).commit()
    }

    /**
     * Adds all the elements of the specified collection to the end of this list.
     */
    override fun addAll(elements: Collection<Mutator<V>>): Boolean {
        return value.addAll(elements.extract()).commit()
    }

    /**
     * Inserts all the elements of the specified collection [elements] into this list at the specified [index].
     */
    override fun addAll(index: Int, elements: Collection<Mutator<V>>): Boolean {
        return value.addAll(index, elements.extract()).commit()
    }

    /**
     * Removes the given element
     *
     * @return true when the element has been removed from the list
     */
    override fun remove(element: Mutator<V>): Boolean {
        return value.remove(element.value).commit()
    }

    /**
     * Removes an element at the specified [index] from the list.
     *
     * NOTICE: modifying the returned element will have no effect
     *
     * @return the element that has been removed.
     */
    override fun removeAt(index: Int): Mutator<V> {
        return value.removeAt(index).child { }.commit()
    }

    /**
     * Removes all of the given [elements]
     */
    override fun removeAll(elements: Collection<Mutator<V>>): Boolean {
        return value.removeAll(elements.extract()).commit()
    }

    /**
     * Retains all of the given [elements]
     */
    override fun retainAll(elements: Collection<Mutator<V>>): Boolean {
        return value.retainAll(elements.extract()).commit()
    }

    /**
     * Get the value at the given index
     */
    override fun get(index: Int): Mutator<V> {
        // We remember the original object
        var last = value[index]

        return value[index].child { new ->
            // Is our element still in the list and where is it?
            // Notice: We need to use the object equality check here '==='
            val pos = value.indexOfFirst { it === last }

            if (pos != -1) {
                last = new
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
        return value.set(index, element.value).child { }.commit()
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
        TODO("Not yet implemented")
    }

    //  HELPERS  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Extracts the values of the given mutators
     */
    private fun Collection<Mutator<V>>.extract(): List<V> = map { it.value }

    /**
     * Sets the [element] at the given [index]
     */
    private fun setAt(index: Int, element: V) {
        value[index] = element
        commit()
    }
}
