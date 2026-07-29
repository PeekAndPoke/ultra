package io.peekandpoke.ultra.common

/**
 * Defines a typed key to be used with [TypedAttributes] and [MutableTypedAttributes]
 *
 * Keys are compared by identity — neither [name] nor `T` takes part, and [name] only shows up in
 * [toString]. Two keys created with the same name are distinct entries, so whoever defines a key
 * must hold it as a singleton (usually in a companion object) and hand that instance around.
 */
class TypedKey<T>(val name: String = "") {
    override fun toString() = name
}

/**
 * Immutable map of [TypedKey] to values
 *
 * [Builder] is the type-safe way in: it ties each value to the type of its key. The primary
 * constructor performs no such check and does not copy the given map, so pass it an immutable one.
 *
 * @property entries The stored keys and their values.
 */
data class TypedAttributes(val entries: Map<TypedKey<*>, Any?>) {

    companion object {
        /** Empty instance */
        val empty = TypedAttributes(emptyMap())

        /** Builder method */
        operator fun invoke(builder: Builder.() -> Unit) = of(builder)

        /** Builder method */
        fun of(builder: Builder.() -> Unit) = Builder().apply(builder).build()
    }

    /** Builder for [TypedAttributes] */
    class Builder {

        private val entries = mutableMapOf<TypedKey<*>, Any?>()

        /**
         * Adds an entry by [key] and [value]
         */
        fun <T> add(key: TypedKey<T>, value: T) {
            entries[key] = value
        }

        /**
         * Builds the [TypedAttributes] instance
         */
        internal fun build() = TypedAttributes(entries.toMap())
    }

    /**
     * Gets the number of entries
     *
     * Determined once when this instance is created.
     */
    val size: Int = entries.size

    /**
     * Converts this to a mutable [MutableTypedAttributes] collection.
     *
     * The entries are copied, so the result is independent of this instance.
     */
    fun asMutable(): MutableTypedAttributes = MutableTypedAttributes(entries)

    /**
     * Gets an entry by [key] or null if nothing is there
     *
     * The value is cast unchecked. Entries added through [Builder] always match their key, but a map
     * handed to the primary constructor is not verified.
     */
    operator fun <T> get(key: TypedKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as T?
    }

    /**
     * Adds an entry by [key] and [value].
     *
     * Returns a new instance of [TypedAttributes].
     */
    fun <T> plus(key: TypedKey<T>, value: T) = TypedAttributes(
        entries.plus(key to value)
    )

    /**
     * Adds all entries from [other], where [other] wins on keys that are set in both.
     *
     * Returns a new instance of [TypedAttributes].
     */
    fun plus(other: TypedAttributes) = copy(
        entries = entries.plus(other.entries)
    )

    /**
     * Adds entries from the [builder], where the built entries win on keys that are set in both.
     *
     * Returns a new instance of [TypedAttributes].
     */
    fun plus(builder: Builder.() -> Unit): TypedAttributes {
        val built = of(builder)

        return copy(
            entries = entries.plus(built.entries)
        )
    }
}

/**
 * Mutable map of [TypedKey] to values
 *
 * Every read and write takes the same [RunSync] lock, so the map is never read while it is being
 * mutated. Callbacks passed in ([getOrPut], [setWhen]) run while that lock is held; re-entering this
 * instance from one of them is safe but the callback must not block on another thread.
 */
class MutableTypedAttributes internal constructor(entries: Map<TypedKey<*>, Any?> = emptyMap()) {

    companion object {
        /**
         * Empty instance
         */
        fun empty(): MutableTypedAttributes = MutableTypedAttributes(emptyMap())

        /**
         * Builder method
         */
        operator fun invoke(builder: Builder.() -> Unit): MutableTypedAttributes = of(builder)

        /**
         * Builder method
         */
        fun of(builder: Builder.() -> Unit): MutableTypedAttributes = Builder().apply(builder).build()
    }

    /**
     * Builder for [MutableTypedAttributes]
     */
    class Builder {

        private val entries = mutableMapOf<TypedKey<*>, Any?>()

        /**
         * Adds an entry by [key] and [value]
         */
        fun <T> add(key: TypedKey<T>, value: T) {
            entries[key] = value
        }

        /**
         * Builds the [MutableTypedAttributes] instance
         */
        fun build() = MutableTypedAttributes(entries.toMap())
    }

    /**
     * The entries
     */
    private val _entries: MutableMap<TypedKey<*>, Any?> = entries.toMutableMap()

    /**
     * Gets the stored entries
     *
     * A snapshot — later writes to this instance do not show up in it.
     */
    val entries get(): Map<TypedKey<*>, Any?> = RunSync(_entries) { _entries.toMap() }

    /**
     * Gets the number of entries
     */
    val size: Int get() = RunSync(_entries) { _entries.size }

    /**
     * Converts this to an immutable [TypedAttributes] collection.
     *
     * The entries are copied, so the result is independent of this instance.
     */
    fun asImmutable(): TypedAttributes = TypedAttributes(RunSync(_entries) { _entries.toMap() })

    /**
     * Gets an entry by [key] or null if nothing is there
     *
     * The value is cast unchecked; entries added through [Builder] or [set] always match their key.
     */
    operator fun <T> get(key: TypedKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return RunSync(_entries) { _entries[key] } as T?
    }

    /**
     * Sets an entry by [key] with the given [value]
     */
    operator fun <T> set(key: TypedKey<T>, value: T) {
        RunSync(_entries) {
            _entries[key] = value
        }
    }

    /**
     * Adds all entries from [other], overwriting keys that are already set.
     *
     * Every entry is set on its own, so the merge as a whole is not atomic.
     */
    fun add(other: TypedAttributes) {
        other.entries.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            set(key as TypedKey<Any?>, value)
        }
    }

    /**
     * Returns 'true' when the given key is set even if the value is falsy, like null, false etc.
     */
    fun <T> has(key: TypedKey<T>) = RunSync(_entries) { _entries.containsKey(key) }

    /**
     * Remove an entry by [key]
     */
    fun <T> remove(key: TypedKey<T>) {
        RunSync(_entries) {
            _entries.remove(key)
        }
    }

    /**
     * Gets the value for the given [key] it the value is present.
     *
     * If the value is not present or null, it will be produced and stored. A stored null therefore
     * counts as absent here, unlike in [has].
     *
     * [produce] runs while the lock is held.
     */
    fun <T> getOrPut(key: TypedKey<T>, produce: () -> T): T {

        return RunSync(_entries) {

            @Suppress("UNCHECKED_CAST")
            when (val value = _entries[key] as T?) {
                null -> produce().also {
                    _entries[key] = it
                }

                else -> value
            }
        }
    }

    /**
     * Sets an entry by [key] with the given [value] when the [condition] is true.
     *
     * The [condition] is executed with the current value for the [key], while the lock is held.
     *
     * Returns 'true' if the value was actually updated.
     */
    fun <T> setWhen(key: TypedKey<T>, value: T, condition: (T?) -> Boolean): Boolean {
        return RunSync(_entries) {
            val current = get(key)

            condition(current).also { result ->
                if (result) {
                    _entries[key] = value
                }
            }
        }
    }

    /**
     * Creates a clone of this instance by shallow cloning the entries.
     *
     * The map is new, the keys and values in it are shared with this instance.
     */
    fun clone() = MutableTypedAttributes(entries = RunSync(_entries) { _entries.toMap() })
}
