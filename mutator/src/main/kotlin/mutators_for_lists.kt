package de.peekandpoke.ultra.mutator

import de.peekandpoke.ultra.common.*

fun <T, M> List<T>.mutator(

    onModify: OnModify<List<T>>,
    backwardMapper: (M) -> T,
    forwardMapper: (T, OnModify<T>) -> M

): ListMutator<T, M> {

    return ListMutator(this, onModify, forwardMapper, backwardMapper)
}

open class ListMutator<T, M>(

    original: List<T>,
    onModify: OnModify<List<T>>,
    private val forwardMapper: (T, OnModify<T>) -> M,
    private val backwardMapper: (M) -> T

) : MutatorBase<List<T>, MutableList<T>>(original, onModify), MutableList<M> {

    /**
     * Replaces the whole lists
     */
    operator fun plusAssign(value: List<M>) = plusAssign(value.map(backwardMapper))

    /**
     * Creates a mutable copy of the given [input]
     */
    override fun copy(input: List<T>) = input.toMutableList()

    override fun iterator(): MutableIterator<M> = It(getResult(), forwardMapper)

    override fun listIterator(): MutableListIterator<M> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listIterator(index: Int): MutableListIterator<M> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<M> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //// MutableList<M> implementation ////////////////////////////////////////////////////////////////////

    /**
     * Returns the size of the list
     */
    override val size get() = getResult().size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = getResult().isEmpty()

    /**
     * Returns true if the list contains the given [element]
     */
    override fun contains(element: M) =
        getResult().contains(backwardMapper(element))

    /**
     * Returns true if the list constains all of the given [elements]
     */
    override fun containsAll(elements: Collection<M>) =
        getResult().containsAll(elements.map(backwardMapper))

    /**
     * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun indexOf(element: M) =
        getResult().indexOf(backwardMapper(element))

    /**
     * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
     * element is not contained in the list.
     */
    override fun lastIndexOf(element: M) =
        getResult().lastIndexOf(backwardMapper(element))

    /**
     * Clears the whole list
     */
    override fun clear() {
        if (size > 0) {
            getMutableResult().clear()
        }
    }

    /**
     * Get the element at the given index
     */
    override operator fun get(index: Int) =
        forwardMapper(getResult()[index]) { set(index, it) }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * NOTICE: modifying the return element will have no effect
     *
     * @return The element previously at the specified position
     */
    override operator fun set(index: Int, element: M) =
        forwardMapper(set(index, backwardMapper(element))) {}

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    override fun add(element: M) =
        getMutableResult().add(backwardMapper(element))

    /**
     * Inserts an element into the list at the specified [index].
     */
    override fun add(index: Int, element: M) =
        getMutableResult().add(index, backwardMapper(element))

    /**
     * Adds all of the elements of the specified collection to the end of this list.
     *
     * The elements are appended in the order they appear in the [elements] collection.
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    override fun addAll(elements: Collection<M>) = when {

        elements.isEmpty() -> false

        else -> getMutableResult().addAll(elements.map(backwardMapper))
    }

    /**
     * Inserts all of the elements of the specified collection [elements] into this list at the specified [index].
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    override fun addAll(index: Int, elements: Collection<M>) = when {

        elements.isEmpty() -> false

        else -> getMutableResult().addAll(index, elements.map(backwardMapper))
    }

    /**
     * Removes the given element
     *
     * @return true when the element has been removed from the list
     */
    override fun remove(element: M) =
        remove(backwardMapper(element))

    /**
     * Removes an element at the specified [index] from the list.
     *
     * NOTICE: modifying the returned element will have no effect
     *
     * @return the element that has been removed.
     */
    override fun removeAt(index: Int) =
        forwardMapper(getMutableResult().removeAt(index)) {}

    /**
     * Removes all given [elements]
     */
    override fun removeAll(elements: Collection<M>) =
        remove(elements.map(backwardMapper))

    /**
     * Retains all given [elements]
     */
    override fun retainAll(elements: Collection<M>) =
        retain(elements.map(backwardMapper))

    //// additional functionality ////////////////////////////////////////////////////////////////////

    /**
     * Add an element at the end of the list
     */
    fun push(vararg element: T) = apply { getMutableResult().push(element) }

    /**
     * Removes the last element from the list and returns it
     */
    fun pop() = if (size > 0) getMutableResult().pop() else null

    /**
     * Add an element at the beginning of the list
     */
    fun unshift(vararg element: T) = apply { getMutableResult().unshift(element) }

    /**
     * Removes the first element from the list and returns it
     */
    fun shift() = if (size > 0) getMutableResult().shift() else null

    /**
     * Removes a specific [element] from the list
     *
     * @return 'true' when the list has been modified
     */
    fun remove(vararg element: T) = remove(element.toList())

    /**
     * Removes the specified [elements] from the list
     *
     * @return 'true' when the list has been modified
     */
    fun remove(elements: List<T>): Boolean {

        if (getResult().containsAny(elements)) {

            getMutableResult().removeAll(elements)

            return true
        }

        return false
    }

    /**
     * Removes all elements from the the list that match the filter
     */
    fun removeWhere(predicate: T.() -> Boolean) = retainWhere { !predicate(this) }

    /**
     * Retains the given [element]s in the list
     *
     * @return 'true' when the list has been modified
     */
    fun retain(vararg element: T) = retain(element.toList())

    /**
     * Retains the given [elements]s in the list
     *
     * @return 'true' when the list has been modified
     */
    fun retain(elements: List<T>): Boolean {

        if (getResult().size != elements.size || !getResult().containsAll(elements)) {

            getMutableResult().retainAll(elements)

            return true
        }

        return false
    }

    /**
     * Retains all elements in the list that match the predicate
     */
    fun retainWhere(predicate: T.() -> Boolean) = apply {

        val filtered = getResult().filter(predicate)

        if (filtered.size < size) {
            plusAssign(filtered)
        }
    }


    /**
     * Set the element at the given index
     */
    @JvmName("set_T")
    operator fun set(index: Int, element: T): T {

        val current = getResult()[index]

        // We only trigger the cloning, when the value has changed
        if (current.isNotSameAs(element)) {
            getMutableResult()[index] = element
        }

        return current
    }

    /**
     * Iterator impl
     */
    internal inner class It(

        private val list: List<T>,
        private val mapper: (T, OnModify<T>) -> M

    ) : MutableIterator<M> {

        private var pos = 0

        override fun hasNext() = pos < list.size

        override fun next(): M {

            val idx = pos++
            val current = list[idx]

            return mapper(current) { set(idx, it) }
        }

        override fun remove() {
            if (pos > 0) {
                removeAt(pos - 1)
            }
        }
    }
}
