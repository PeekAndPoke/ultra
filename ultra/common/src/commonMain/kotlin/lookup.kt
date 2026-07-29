package io.peekandpoke.ultra.common

import kotlin.reflect.KClass

/**
 * A lookup is a mapping from a class to an instance of that class.
 *
 * Entries are addressed by their exact class: an instance registered under a subclass is not found
 * when asking for its supertype, and the other way round.
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
 * Simple implementation of [Lookup], backed by a map of runtime class to instance.
 *
 * [provider] is called once on the first access and its result is kept, so items added to its source
 * afterwards are not picked up.
 */
class SimpleLookup<T : Any>(provider: () -> List<T>) : Lookup<T> {

    /**
     * Map of item classes to items
     *
     * Keyed by the runtime class of each item, so two items of the same class collapse into one
     * entry — the last one provided wins, and the earlier ones are dropped from [all] as well.
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
     * Returns all instances, at most one per class
     */
    override fun all(): List<T> = items.values.toList()
}
