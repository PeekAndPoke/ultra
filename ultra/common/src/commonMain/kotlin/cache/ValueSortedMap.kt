package de.peekandpoke.ultra.common.cache

/**
 * A map with fast key lookups and iteration ordered by a sortable projection of V.
 * Stable among equal sort keys via a monotonic id.
 */
internal class ValueSortedMap<K, V, T : Comparable<T>>(
    private val sortKeyOf: (V) -> T,
) : MutableMap<K, V> {

    private data class Node<K, V, T : Comparable<T>>(
        val key: K,
        val value: V,
        val sort: T,
        val id: Long, // tie-breaker to keep order deterministic among equals
    )

    /**
     * Small helper to expose entries as MutableEntry without backing linkage.
     */
    private class SimpleEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val old = value
            value = newValue
            return old
        }
    }

    private val byKey = HashMap<K, Node<K, V, T>>()    // O(1) lookups by key
    private val sorted = ArrayList<Node<K, V, T>>()    // kept sorted by (sort, id)

    private var nextId = 0L

    private val nodeComparator = Comparator<Node<K, V, T>> { a, b ->
        val c = a.sort.compareTo(b.sort)
        if (c != 0) c else a.id.compareTo(b.id)
    }

    /**
     * Find the position of a node by in the sorted array.
     */
    private fun indexOfInSorted(node: Node<K, V, T>): Int {
        return sorted.binarySearch(node, nodeComparator)
    }

    // --- MutableMap<K, V> ---

    override val size: Int get() = byKey.size

    override fun isEmpty(): Boolean = byKey.isEmpty()

    override fun containsKey(key: K): Boolean = byKey.containsKey(key)

    override fun containsValue(value: V): Boolean = byKey.values.any { it.value == value }

    override fun get(key: K): V? = byKey[key]?.value

    override val keys: MutableSet<K> get() = byKey.keys

    override val values: MutableCollection<V> get() = sorted.mapTo(ArrayList()) { it.value }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = sorted
            .map { SimpleEntry(it.key, it.value) }
            .toMutableSet()

    override fun clear() {
        byKey.clear()
        sorted.clear()
    }

    override fun put(key: K, value: V): V? {
        val prev = byKey[key]
        if (prev != null) {
            // remove old node from the sorted array
            val idx = indexOfInSorted(prev)
            if (idx >= 0) sorted.removeAt(idx)
        }

        val node = Node(key, value, sortKeyOf(value), nextId++)
        byKey[key] = node

        val ins = run {
            val idx = sorted.binarySearch(node, nodeComparator)
            if (idx >= 0) idx else -(idx + 1)
        }
        sorted.add(ins, node)

        return prev?.value
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (k, v) -> put(k, v) }
    }

    override fun remove(key: K): V? {
        val node = byKey.remove(key) ?: return null
        val idx = indexOfInSorted(node)

        if (idx >= 0) sorted.removeAt(idx)

        return node.value
    }

    /** Entries in ascending order by sort key. */
    fun ascending(): Iterable<Pair<K, V>> = object : Iterable<Pair<K, V>> {
        override fun iterator() = object : Iterator<Pair<K, V>> {
            private var i = 0
            override fun hasNext() = i < sorted.size
            override fun next(): Pair<K, V> = sorted[i++].let { it.key to it.value }
        }
    }

    /** Entries in descending order by sort key. */
    fun descending(): Iterable<Pair<K, V>> = object : Iterable<Pair<K, V>> {
        override fun iterator() = object : Iterator<Pair<K, V>> {
            private var i = sorted.size - 1
            override fun hasNext() = i >= 0
            override fun next(): Pair<K, V> = sorted[i--].let { it.key to it.value }
        }
    }
}
