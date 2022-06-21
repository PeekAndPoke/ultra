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

    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    override fun <X : T> get(cls: KClass<X>): X = getOrNull(cls) ?: error("There is no service of type '$cls'")

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> getOrNull(cls: KClass<X>): X? = map[cls]?.invoke(kontainer, context) as X?

    override fun all(): List<T> {
        return map.keys.map { get(it) }
    }
}
