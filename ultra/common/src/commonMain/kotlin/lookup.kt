package de.peekandpoke.ultra.common

import kotlin.reflect.KClass

/**
 * A lookup is a mapping from a class to an instance of that class.
 */
interface Lookup<T : Any> {

    /**
     * Returns 'true' when the lookup contains an instance of the given [cls]
     */
    fun <X : T> has(cls: KClass<X>): Boolean

    /**
     * Returns the instance of the given [cls]
     *
     * Throws an exception when there is no instance for the requested [cls]
     */
    fun <X : T> get(cls: KClass<X>): X

    /**
     * Returns the instance of the given [cls]
     *
     * When there is no instance for the requested [cls] then null is returned
     */
    fun <X : T> getOrNull(cls: KClass<X>): X?

    /**
     * Returns all instances
     */
    fun all(): List<T>
}

/**
 * Simple implementation of [Lookup]
 */
class SimpleLookup<T : Any>(provider: () -> List<T>) : Lookup<T> {

    /**
     * Map of item classes to items
     */
    private val items: Map<KClass<out T>, T> by lazy {
        provider().associateBy { it::class }
    }

    /**
     * Returns 'true' when the lookup contains an instance of the given [cls]
     */
    override fun <X : T> has(cls: KClass<X>): Boolean = items.contains(cls)

    /**
     * Returns the instance of the given [cls]
     *
     * Throws an exception when there is no instance for the requested [cls]
     */
    override fun <X : T> get(cls: KClass<X>): X = getOrNull(cls)
        ?: error("There is no instance of '$cls'")

    /**
     * Returns the instance of the given [cls]
     *
     * When there is no instance for the requested [cls] then null is returned
     */
    @Suppress("UNCHECKED_CAST")
    override fun <X : T> getOrNull(cls: KClass<X>): X? = items[cls] as X?

    /**
     * Returns all instances
     */
    override fun all(): List<T> = items.values.toList()
}
