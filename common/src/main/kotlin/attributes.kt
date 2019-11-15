package de.peekandpoke.ultra.common

/**
 * Defines a typed key to be used with [TypedAttributes]
 */
@Suppress("unused")
class TypedKey<T>(val name: String = "")

/**
 * Map of [TypedKey] to values
 */
data class TypedAttributes internal constructor(val entries: Map<TypedKey<*>, Any>) {

    companion object {
        /**
         * Empty instance
         */
        val empty = TypedAttributes(emptyMap())

        /**
         * Builder method
         */
        fun of(builder: Builder.() -> Unit) = Builder().apply(builder).build()
    }

    /**
     * Builder for [TypedAttributes]
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
         * Builds the [TypedAttributes] instance
         */
        fun build() = TypedAttributes(entries.toMap())
    }

    /**
     * Gets an entry by [key] or null if nothing is there
     */
    operator fun <T : Any> get(key: TypedKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as T
    }
}
