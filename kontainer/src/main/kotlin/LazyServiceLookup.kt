package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * Lazy [Lookup] implementation
 */
class LazyServiceLookup<T : Any>(
    /** The injection context */
    private val context: InjectionContext,
    /** Map from class to function that produces a service */
    private val map: Map<KClass<out T>, (InjectionContext) -> T>

) : Lookup<T> {

    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> get(cls: KClass<X>): X? = map[cls]?.invoke(context) as X?

    override fun all(): List<T> {
        return map.keys.map { get(it)!! }
    }
}

/**
 * The blueprint is a pre-stage of the [LazyServiceLookup] and can be re-used with a [InjectionContext]
 */
class LazyServiceLookupBlueprint<T : Any>(
    private val map: Map<KClass<out T>, (InjectionContext) -> T>
) {
    /**
     * Creates a [LazyServiceLookup] for the given [context]
     */
    fun with(context: InjectionContext) = LazyServiceLookup(context, map)
}
