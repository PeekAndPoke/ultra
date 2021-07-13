package de.peekandpoke.ultra.common

/**
 * Map of [TypedKey] to values
 */
class MutableTypedAttributes internal constructor(entries: Map<TypedKey<*>, Any?> = emptyMap()) {

    companion object {
        /**
         * Empty instance
         */
        fun empty() = MutableTypedAttributes(emptyMap())

        /**
         * Builder method
         */
        operator fun invoke(builder: Builder.() -> Unit) = of(builder)

        /**
         * Builder method
         */
        fun of(builder: Builder.() -> Unit) = Builder().apply(builder).build()
    }

    /**
     * Builder for [MutableTypedAttributes]
     */
    class Builder {

        private val entries = mutableMapOf<TypedKey<*>, Any>()

        /**
         * Adds an entry by [key] and [value]
         */
        fun <T : Any> add(key: TypedKey<T>, value: T) {
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
    private val entries: MutableMap<TypedKey<*>, Any?> = entries.toMutableMap()

    /**
     * Gets the number of entries
     */
    val size get() = entries.size

    /**
     * Converts this to an immutable [TypedAttributes] collection.
     */
    fun asImmutable() = TypedAttributes(entries.toMap())

    /**
     * Gets an entry by [key] or null if nothing is there
     */
    operator fun <T> get(key: TypedKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as T?
    }

    /**
     * Sets an entry by [key] with the given [value]
     */
    operator fun <T> set(key: TypedKey<T>, value: T) {
        RunSync(entries) {
            entries[key] = value
        }
    }

    /**
     * Returns 'true' when the given key is set even if the value is falsy, like null, false etc.
     */
    fun <T> has(key: TypedKey<T>) = entries.containsKey(key)

    /**
     * Remove an entry by [key]
     */
    fun <T> remove(key: TypedKey<T>) {
        RunSync(entries) {
            entries.remove(key)
        }
    }

    /**
     * Sets an entry by [key] with the given [value] when the [condition] is true.
     *
     * The [condition] is executed with the current value for the [key].
     *
     * Returns 'true' if the value was actually updated.
     */
    fun <T> setWhen(key: TypedKey<T>, value: T, condition: (T?) -> Boolean): Boolean {
        return RunSync(entries) {
            val current = get(key)

            condition(current).also { result ->
                if (result) {
                    entries[key] = value
                }
            }
        }
    }

    /**
     * Creates a clone of this instance by shallow cloning the contained [entries]
     */
    fun clone() = MutableTypedAttributes(entries = entries.toMap())
}
