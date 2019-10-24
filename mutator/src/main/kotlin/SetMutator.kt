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

) : MutatorBase<Set<T>, MutableSet<T>>(original, onModify), Iterable<M> {

    operator fun plusAssign(value: List<M>) = plusAssign(value.map(backwardMapper).toSet())

    override fun copy(input: Set<T>): MutableSet<T> = MutableSetWrapper(input.toMutableSet())

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
     * Adds elements to the set
     */
    fun add(vararg element: T) = apply { getMutableResult().addAll(element) }

    /**
     * Removes elements from the set
     */
    fun remove(vararg element: T) = apply {

        if (getResult().containsAny(element)) {
            getMutableResult().removeAll(element)
        }
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
    internal inner class It(set: Set<T>, private val mapper: (T, OnModify<T>) -> M) : Iterator<M> {

        private val inner = set.toList().iterator()

        override fun hasNext() = inner.hasNext()

        override fun next(): M {

            val current = inner.next()

            return mapper(current) {
                remove(current)
                add(it)
            }
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
