package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * Lazy [Lookup] implementation
 */
class LazyServiceLookup<T : Any>(
    /** The kontainer */
    private val kontainer: Kontainer,
    /** The context */
    private val context: InjectionContext,
    /** Map from class to function that produces a service */
    private val map: Map<KClass<out T>, (Kontainer, InjectionContext) -> T>,
) : Lookup<T> {

    /** All service classes available in this lookup */
    val classes: Set<KClass<out T>> = map.keys

    /** Returns true if a service of the given [cls] is available */
    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    /** Gets the service for [cls] or throws if not found */
    override fun <X : T> get(cls: KClass<X>): X = getOrNull(cls) ?: error("There is no service of type '$cls'")

    /** Gets the service for [cls] or null if not found */
    @Suppress("UNCHECKED_CAST")
    override fun <X : T> getOrNull(cls: KClass<X>): X? = map[cls]?.invoke(kontainer, context) as X?

    /** Returns all services in this lookup */
    override fun all(): List<T> {
        return classes.map { get(it) }
    }
}
