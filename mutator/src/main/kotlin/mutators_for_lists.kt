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

) : MutatorBase<List<T>, MutableList<T>>(original, onModify), Iterable<M> {

    operator fun plusAssign(value: List<M>) = plusAssign(value.map(backwardMapper))

    override fun copy(input: List<T>) = input.toMutableList()

    override fun iterator(): Iterator<M> = It(getResult(), forwardMapper)

    /**
     * Returns the size of the list
     */
    val size get() = getResult().size

    /**
     * Returns true when the list is empty
     */
    fun isEmpty() = getResult().isEmpty()

    /**
     * Clears the whole list
     */
    fun clear() = apply {
        if (size > 0) {
            getMutableResult().clear()
        }
    }

    /**
     * Add an element at the end of the list
     */
    fun push(vararg element: T) = apply { getMutableResult().push(*element) }

    /**
     * Removes the last element from the list and returns it
     */
    fun pop() = if (size > 0) getMutableResult().pop() else null

    /**
     * Add an element at the beginning of the list
     */
    fun unshift(vararg element: T) = apply { getMutableResult().unshift(*element) }

    /**
     * Removes the first element from the list and returns it
     */
    fun shift() = if (size > 0) getMutableResult().shift() else null

    /**
     * Removes a specific element from the list
     */
    fun remove(vararg element: T) = apply {

        if (getResult().containsAny(*element)) {
            getMutableResult().removeAll(element)
        }
    }

    /**
     * Removes the element at the given index
     */
    fun removeAt(index: Int) = apply {

        if (isInBounds(index)) {
            getMutableResult().removeAt(index)
        }
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
     * Removes all elements from the the list that match the filter
     */
    fun removeWhere(predicate: T.() -> Boolean) = retainWhere { !predicate(this) }

    /**
     * Get the element at the given index
     */
    operator fun get(index: Int): M = forwardMapper(getResult()[index]) { set(index, it) }

    /**
     * Set the element at the given index
     */
    operator fun set(index: Int, element: T) = apply {

        val current = getResult()[index]

        // We only trigger the cloning, when the value has changed
        if (current.isNotSameAs(element)) {
            getMutableResult()[index] = element
        }
    }

    private fun isInBounds(idx: Int): Boolean {
        @Suppress("ConvertTwoComparisonsToRangeCheck")
        return idx >= 0 && idx < size
    }

    /**
     * Iterator impl
     */
    internal inner class It(private val list: List<T>, private val mapper: (T, OnModify<T>) -> M) : Iterator<M> {

        private var pos = 0

        override fun hasNext() = pos < list.size

        override fun next(): M {

            val idx = pos++
            val current = list[idx]

            return mapper(current) { set(idx, it) }
        }
    }
}
