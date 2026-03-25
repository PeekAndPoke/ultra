package io.peekandpoke.mutator

fun <K, V> Map<K, V>.mutator(child: V.() -> Mutator<V>): MapMutator<K, V> = MapMutatorImpl(this, child)

interface MapMutator<K, V> : Mutator<MutableMap<K, V>>, MutableMap<K, Mutator<V>> {
    fun getChildMutator(child: V): Mutator<V>
}

class MapMutatorImpl<K, V>(
    initial: Map<K, V>,
    private val childToMutator: V.() -> Mutator<V>,
) : Mutator.Base<MutableMap<K, V>>(initial.toMutableMap()), MapMutator<K, V> {

    override fun getChildMutator(child: V): Mutator<V> = childToMutator(child)

    override val entries: MutableSet<MutableMap.MutableEntry<K, Mutator<V>>>
        get() = doGet().entries.map { (key, value) ->
            object : MutableMap.MutableEntry<K, Mutator<V>> {
                override val key: K = key
                override val value: Mutator<V> = value.childToMutator().onChange { new ->
                    if (doGet()[key] != new) {
                        doGet()[key] = new
                        notifyObservers()
                    }
                }

                override fun setValue(newValue: Mutator<V>): Mutator<V> {
                    val old = this.value
                    if (doGet()[key] != newValue.get()) {
                        doGet()[key] = newValue.get()
                        notifyObservers()
                    }
                    return old
                }
            }
        }.toMutableSet()

    override val keys: MutableSet<K>
        get() = object : AbstractMutableSet<K>() {
            private val delegate = doGet().keys.toSet()
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<K> = object : MutableIterator<K> {
                private val inner = delegate.iterator()
                override fun hasNext(): Boolean = inner.hasNext()
                override fun next(): K = inner.next()
                override fun remove() {
                    throw UnsupportedOperationException("MapMutator.keys is a read-only view")
                }
            }

            override fun add(element: K): Boolean {
                throw UnsupportedOperationException("MapMutator.keys is a read-only view")
            }
        }

    override val size: Int get() = doGet().size

    override val values: MutableCollection<Mutator<V>>
        get() = object : AbstractMutableCollection<Mutator<V>>() {
            private val delegate = entries.map { it.value }
            override val size: Int get() = delegate.size
            override fun iterator(): MutableIterator<Mutator<V>> = object : MutableIterator<Mutator<V>> {
                private val inner = delegate.iterator()
                override fun hasNext(): Boolean = inner.hasNext()
                override fun next(): Mutator<V> = inner.next()
                override fun remove() {
                    throw UnsupportedOperationException("MapMutator.values is a read-only view")
                }
            }

            override fun add(element: Mutator<V>): Boolean {
                throw UnsupportedOperationException("MapMutator.values is a read-only view")
            }
        }

    override fun clear() {
        if (doGet().isNotEmpty()) {
            doGet().clear()
            notifyObservers()
        }
    }

    override fun isEmpty(): Boolean = doGet().isEmpty()

    override fun remove(key: K): Mutator<V>? {
        if (!doGet().containsKey(key)) return null

        val removed = doGet().remove(key)
        notifyObservers()

        return removed?.childToMutator()
    }

    override fun putAll(from: Map<out K, Mutator<V>>) {
        var changed = false
        from.forEach { (k, v) ->
            if (doGet()[k] != v.get()) {
                doGet()[k] = v.get()
                changed = true
            }
        }
        if (changed) {
            notifyObservers()
        }
    }

    override fun put(key: K, value: Mutator<V>): Mutator<V>? {
        val old = doGet()[key]
        val newValue = value.get()

        if (old != newValue) {
            doGet()[key] = newValue
            notifyObservers()
        }

        return old?.childToMutator()
    }

    override fun get(key: K): Mutator<V>? {
        val initial = doGet()[key] ?: return null

        return initial.childToMutator().onChange { new ->
            if (doGet()[key] != new) {
                doGet()[key] = new
                notifyObservers()
            }
        }
    }

    override fun containsValue(value: Mutator<V>): Boolean {
        return doGet().containsValue(value.get())
    }

    override fun containsKey(key: K): Boolean {
        return doGet().containsKey(key)
    }

    /**
     * Helper to work around compiler issues with casting to MutableSet<V>
     */
    @Suppress("USELESS_CAST", "NOTHING_TO_INLINE")
    private inline fun doGet(): MutableMap<K, V> = get() as MutableMap<K, V>
}
