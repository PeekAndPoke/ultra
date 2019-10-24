package de.peekandpoke.ultra.mutator

import de.peekandpoke.ultra.common.containsAny

/**
 * Creates a [SetMutator] for this Set
 */
fun <T, M> Set<T>.mutator(

    /** onModify callback */
    onModify: OnModify<Set<T>>,
    /** The backwardMapper maps any mutator [M] back to its value [T] */
    backwardMapper: (M) -> T,
    /** The forwardMapper maps any [T] to is mutator [M] */
    forwardMapper: (T, OnModify<T>) -> M

): SetMutator<T, M> {

    return SetMutator(this, onModify, forwardMapper, backwardMapper)
}

/**
 * Mutator implementation for sets
 */
class SetMutator<T, M>(

    /** the input value, the original set */
    original: Set<T>,
    /** onModify callback */
    onModify: OnModify<Set<T>>,
    /** The forwardMapper maps any [T] to is mutator [M] */
    private val forwardMapper: (T, OnModify<T>) -> M,
    /** The backwardMapper maps any mutator [M] back to its value [T] */
    private val backwardMapper: (M) -> T

) : MutatorBase<Set<T>, MutableSet<T>>(original, onModify), MutableSet<M> {

    /**
     * Replaces the whole list
     */
    operator fun plusAssign(value: List<M>) = plusAssign(value.map(backwardMapper).toSet())

    /**
     * Creates a mutable copy of the given [input]
     */
    override fun copy(input: Set<T>): MutableSet<T> = MutableSetWrapper(input.toMutableSet())

    /**
     * Returns an iterator over all elements mapped to [M]
     */
    override fun iterator(): MutableIterator<M> = It(getResult(), forwardMapper)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  MutableSet<M> implementation
    /////

    /**
     * Returns the size of the list
     */
    override val size get() = getResult().size

    /**
     * Returns true when the list is empty
     */
    override fun isEmpty() = getResult().isEmpty()

    /**
     * Returns true if the set contains the given [element]
     *
     * TODO: tests
     */
    override fun contains(element: M) = contains(backwardMapper(element))

    /**
     * Returns true if the set contains the given [element]
     *
     * TODO: tests, move down
     */
    @JvmName("contains_T")
    fun contains(element: T) = getResult().contains(element)

    /**
     * Returns true if the set contains all of the given [elements]
     *
     * TODO: tests
     */
    override fun containsAll(elements: Collection<M>) = containsAll(elements.map(backwardMapper))

    /**
     * Returns true if the set contains all of the given [elements]
     *
     * TODO: tests, move down
     */
    @JvmName("containsAll_T")
    fun containsAll(elements: Collection<T>) = getResult().containsAll(elements)

    /**
     * Clears the whole list
     */
    override fun clear() {
        if (size > 0) {
            getMutableResult().clear()
        }
    }

    /**
     * Adds the specified element to the set.
     *
     * TODO: tests
     *
     * @return `true` if the element has been added, `false` if the element is already contained in the set.
     */
    override fun add(element: M) = add(backwardMapper(element))

    /**
     * Adds [elements] to the set
     *
     * TODO: tests
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    override fun addAll(elements: Collection<M>) = add(elements.map(backwardMapper))

    /**
     * Removes an [element] from the set
     *
     * TODO: tests
     *
     * @return 'true' of the element was removed from the set
     */
    override fun remove(element: M) = remove(backwardMapper(element))

    /**
     * Removes the specified [elements] from the list
     *
     * TODO: tests
     *
     * @return 'true' when the list has been modified
     */
    override fun removeAll(elements: Collection<M>) = removeAll(elements.map(backwardMapper))

    /**
     * Retains all of the given [elements]
     *
     * TODO: tests
     *
     * @return 'true' when the list has been modified
     */
    override fun retainAll(elements: Collection<M>) = retainAll(elements.map(backwardMapper))

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  additional implementation
    /////

    /**
     * Adds [element]s to the set
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    fun add(vararg element: T) = add(element.toList())

    /**
     * Adds [elements] to the set
     *
     * TODO: tests
     *
     * @return 'true if any of the elements has been added, 'false' if all elements are already contained in the set.
     */
    fun add(elements: Collection<T>): Boolean = when {

        getResult().containsAll(elements) -> false

        else -> getMutableResult().addAll(elements)
    }

    /**
     * Removes elements from the set
     */
    fun remove(vararg element: T) = removeAll(element.toList())

    /**
     * Removes the specified [elements] from the list
     *
     * TODO: tests
     *
     * @return 'true' when the list has been modified
     */
    @JvmName("removeAll_T")
    fun removeAll(elements: Collection<T>): Boolean = when {

        getResult().containsAny(elements) -> getMutableResult().removeAll(elements)

        else -> false
    }

    /**
     * Retains the given [element]s in the list
     *
     * TODO: tests
     *
     * @return 'true' when the list has been modified
     */
    fun retain(vararg element: T) = retainAll(element.toList())

    /**
     * Retains the given [elements]s in the list
     *
     * TODO: tests
     *
     * @return 'true' when the list has been modified
     */
    @JvmName("retainAll_T")
    fun retainAll(elements: Collection<T>): Boolean = when {

        getResult().size != elements.size ||
                !getResult().containsAll(elements) -> getMutableResult().retainAll(elements)

        else -> false
    }

    /**
     * Retains all elements in the list that match the predicate
     */
    fun retainWhere(predicate: T.() -> Boolean) = apply {

        val filtered = getResult().filter(predicate)

        if (filtered.size < size) {
            plusAssign(filtered.toSet())
        }
    }

    /**
     * Removes all elements from the the list that match the predicate
     */
    fun removeWhere(predicate: T.() -> Boolean) = retainWhere { !predicate(this) }

    /**
     * Iterator impl
     */
    internal inner class It(set: Set<T>, private val mapper: (T, OnModify<T>) -> M) : MutableIterator<M> {

        private val inner = set.toList().iterator()
        private var current: T? = null

        override fun hasNext() = inner.hasNext()

        override fun next(): M {

            return inner.next().run {
                // remember the current element, so we can use it for remove()
                current = this
                // return the current element mapped to a mutator with onModify callback
                mapper(this) {
                    remove(this)
                    add(it)
                }
            }
        }

        override fun remove() {
            current?.apply { remove(this) }
        }
    }
}

internal class MutableSetWrapper<X>(inner: MutableSet<X>) : MutableSet<X> {

    private var _inner = inner

    private var primed = false

    override fun toString() = _inner.toString()

    override fun equals(other: Any?) = repair { _inner == other }

    override fun hashCode() = repair { _inner.hashCode() }

    override val size: Int get() = _inner.size

    //// Safe operations ////////////////////////////////////////////////////////////////////

    override fun iterator() = _inner.iterator()

    override fun contains(element: X) = _inner.contains(element)

    override fun containsAll(elements: Collection<X>) = _inner.containsAll(elements)

    override fun isEmpty() = _inner.isEmpty()

    //// Ops that prime reparations /////////////////////////////////////////////////////////

    override fun add(element: X) = prime { _inner.add(element) }

    override fun addAll(elements: Collection<X>) = prime { _inner.addAll(elements) }

    override fun clear() = prime { _inner.clear() }

    override fun remove(element: X) = prime { _inner.remove(element) }

    override fun removeAll(elements: Collection<X>) = prime { _inner.removeAll(elements) }

    override fun retainAll(elements: Collection<X>) = prime { _inner.retainAll(elements) }

    //// helpers ////////////////////////////////////////////////////////////////////////////

    private fun <R> prime(cb: () -> R): R {

        primed = true

        return cb()
    }

    private fun <R> repair(cb: () -> R): R {

        if (primed) {
            primed = false

            // Here we restore the inner hash tables of the set
            _inner = LinkedHashSet(_inner)
        }

        return cb()
    }
}
