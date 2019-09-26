package de.peekandpoke.ultra.common

import kotlin.reflect.KClass

class SimpleLookup<T : Any>(items: () -> List<T>) : Lookup<T> {

    private val inner by lazy {
        items().map { it::class to it }.toMap()
    }

    override fun <X : T> has(cls: KClass<X>): Boolean = inner.contains(cls)

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> get(cls: KClass<X>): X? = inner[cls] as X?

    override fun all(): List<T> = inner.values.toList()
}
