package de.peekandpoke.ultra.mutator

fun <K, T, M> Map<K, T>.mutator(

    onModify: OnModify<Map<K, T>>,
    backwardMapper: (M) -> T,
    forwardMapper: (T, OnModify<T>) -> M

): MapMutator<K, T, M> {

    return MapMutator(this, onModify, forwardMapper, backwardMapper)
}

@Suppress("Detekt.TooManyFunctions")
class MapMutator<K, T, M>(

    original: Map<K, T>,
    onModify: OnModify<Map<K, T>>,
    private val forwardMapper: (T, OnModify<T>) -> M,
    private val backwardMapper: (M) -> T

) : MutatorBase<Map<K, T>, MutableMap<K, T>>(original, onModify), Iterable<Map.Entry<K, M>> {

    operator fun plusAssign(value: Map<K, M>) = plusAssign(value.entries.toList())

    operator fun plusAssign(value: List<Map.Entry<K, M>>) = plusAssign(
        value.associate { kv -> kv.key to backwardMapper(kv.value) }
    )

    override fun copy(input: Map<K, T>) = input.toMutableMap()

    override fun iterator(): Iterator<Map.Entry<K, M>> = It(getResult(), forwardMapper)

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
    fun clear() = apply { getMutableResult().clear() }

    /**
     * Put multiple elements into the map
     */
    fun put(vararg pair: Pair<K, T>) = apply { pair.forEach { (k, v) -> set(k, v) } }

    /**
     * Remove elements from the map by their keys
     */
    fun remove(vararg key: K) = apply {

        val contained = key.filter { getResult().containsKey(it) }

        if (contained.isNotEmpty()) {
            getMutableResult().apply {
                contained.forEach { k ->
                    remove(k)
                }
            }
        }
    }

    /**
     * Retains all elements in the list that match the filter
     */
    fun retainWhere(filter: (Map.Entry<K, T>) -> Boolean) = apply { plusAssign(getResult().filter(filter).toMap()) }

    /**
     * Removes all elements from the the list that match the filter
     */
    fun removeWhere(filter: (Map.Entry<K, T>) -> Boolean) = apply { retainWhere { !filter(it) } }

    /**
     * Get the element at the given index
     */
    operator fun get(index: K): M? = getResult()[index]?.let { entry -> forwardMapper(entry) { set(index, it) } }

    /**
     * Set the element at the given index
     */
    operator fun set(index: K, element: T) = apply {

        val current = getResult()[index]

        // We only trigger the cloning, when the value has changed
        if (current == null || current.isNotSameAs(element)) {
            getMutableResult()[index] = element
        }
    }

    /**
     * Map.Entry impl
     */
    internal data class Entry<KX, VX>(override val key: KX, override val value: VX) : Map.Entry<KX, VX>

    /**
     * Iterator impl
     */
    internal inner class It(map: Map<K, T>, private val mapper: (T, OnModify<T>) -> M) : Iterator<Map.Entry<K, M>> {

        private val inner = map.iterator()

        override fun hasNext() = inner.hasNext()

        override fun next(): Map.Entry<K, M> {

            val next = inner.next()

            return Entry(next.key, mapper(next.value) { set(next.key, it) })
        }
    }
}
