package io.peekandpoke.ultra.cache

/**
 * A map with fast key lookups and iteration ordered by a sortable projection of V.
 * Stable among equal sort keys via a monotonic id.
 *
 * Deliberately NOT a `MutableMap`. It cannot honour that contract — `values`/`entries` would have to
 * be live views over two indices — and [FastCache], its only caller, needs six members. Declaring
 * the interface only bought silently-broken `keys`/`values`/`entries`.
 *
 * Not thread-safe.
 *
 * @param sortKeyOf projects a value onto its sort key; called once per [put], and the result is snapshotted
 */
internal class ValueSortedMap<K, V, T : Comparable<T>>(
    private val sortKeyOf: (V) -> T,
) {

    /** One entry: the [value] plus its [sort] key snapshotted at insert time and a unique [id]. */
    private data class Node<K, V, T : Comparable<T>>(
        val key: K,
        val value: V,
        val sort: T,
        val id: Long, // tie-breaker to keep order deterministic among equals
    )

    private val byKey = HashMap<K, Node<K, V, T>>() // O(1) lookups by key
    private val sorted = ArrayList<Node<K, V, T>>() // kept sorted by (sort, id)

    private var nextId = 0L

    /** Bumped by every structural change, so the iterators can fail fast instead of skipping entries. */
    private var modCount = 0

    /** Orders by sort key, then by id, so no two distinct nodes ever compare equal. */
    private val nodeComparator = Comparator<Node<K, V, T>> { a, b ->
        val c = a.sort.compareTo(b.sort)
        if (c != 0) c else a.id.compareTo(b.id)
    }

    /**
     * Returns the index of [node] in the sorted array.
     *
     * A miss is impossible while the array is ordered by [nodeComparator], so it is reported rather
     * than skipped — swallowing it would orphan the node and let the two indices drift apart
     * permanently.
     */
    private fun indexOfInSorted(node: Node<K, V, T>): Int {
        val idx = sorted.binarySearch(node, nodeComparator)

        if (idx < 0) {
            error(
                "ValueSortedMap invariant broken: node for key <${node.key}> is in the key index but " +
                        "not findable in the sorted array. The sort key's compareTo is most likely inconsistent."
            )
        }

        return idx
    }

    val size: Int get() = byKey.size

    fun isEmpty(): Boolean = byKey.isEmpty()

    fun isNotEmpty(): Boolean = byKey.isNotEmpty()

    fun containsKey(key: K): Boolean = byKey.containsKey(key)

    fun containsValue(value: V): Boolean = byKey.values.any { it.value == value }

    operator fun get(key: K): V? = byKey[key]?.value

    fun clear() {
        byKey.clear()
        sorted.clear()
        // nextId is deliberately NOT reset - a recycled id could collide with a surviving node
        modCount++
    }

    /**
     * Inserts or replaces the value for [key] and returns the previous value.
     *
     * A replacement gets a fresh id, so it moves behind all entries that share its sort key.
     */
    fun put(key: K, value: V): V? {
        val prev = byKey[key]
        if (prev != null) {
            // remove old node from the sorted array
            sorted.removeAt(indexOfInSorted(prev))
        }

        val node = Node(key, value, sortKeyOf(value), nextId++)
        byKey[key] = node

        val ins = run {
            val idx = sorted.binarySearch(node, nodeComparator)
            // a fresh id never matches an existing node, so this is always the insertion point
            if (idx >= 0) idx else -(idx + 1)
        }
        sorted.add(ins, node)
        modCount++

        return prev?.value
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun putAll(from: Map<out K, V>) {
        from.forEach { (k, v) -> put(k, v) }
    }

    fun remove(key: K): V? {
        val node = byKey.remove(key) ?: return null

        sorted.removeAt(indexOfInSorted(node))
        modCount++

        return node.value
    }

    /**
     * Entries in ascending order by sort key, over the LIVE array.
     *
     * Fails fast if the map is structurally modified while an iterator is open, so mutating mid-walk
     * raises instead of silently skipping entries. Callers that evict while walking must materialise
     * first — `takeWhile` does, being eager on [Iterable].
     */
    fun ascending(): Iterable<Pair<K, V>> = object : Iterable<Pair<K, V>> {
        override fun iterator() = object : Iterator<Pair<K, V>> {
            private val expectedModCount = modCount
            private var i = 0

            override fun hasNext(): Boolean {
                checkForComodification()
                return i < sorted.size
            }

            override fun next(): Pair<K, V> {
                checkForComodification()
                return sorted[i++].let { it.key to it.value }
            }

            private fun checkForComodification() {
                if (modCount != expectedModCount) {
                    error("ValueSortedMap was modified while an ascending() iterator was open")
                }
            }
        }
    }

    /**
     * Entries in descending order by sort key, over a SNAPSHOT.
     *
     * Snapshotted because a reverse cursor cannot survive the array shrinking underneath it. Only
     * [ascending] is on a hot path, so the copy is not worth avoiding here.
     */
    fun descending(): Iterable<Pair<K, V>> =
        sorted.asReversed().map { it.key to it.value }
}
