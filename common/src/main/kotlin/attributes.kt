package de.peekandpoke.ultra.common

data class TypedKey<T>(val name: String = "")

data class TypedAttributes internal constructor(val entries: Map<TypedKey<*>, Any>) {

    companion object {
        val empty = TypedAttributes(emptyMap())

        fun of(builder: Builder.() -> Unit) = Builder().apply(builder).build()
    }

    class Builder {

        private val entries = mutableMapOf<TypedKey<*>, Any>()

        fun <T : Any> add(key: TypedKey<T>, value: T) {
            entries[key] = value
        }

        fun build() = TypedAttributes(entries.toMap())
    }

    operator fun <T : Any> get(key: TypedKey<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return entries[key] as T
    }
}
