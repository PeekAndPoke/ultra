package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

class LazyServiceLookup<T : Any>(

    private val context: InjectionContext,
    private val map: Map<KClass<out T>, (InjectionContext) -> T>

) : Lookup<T> {

    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> get(cls: KClass<X>): X? = map[cls]?.invoke(context) as X?

    override fun all(): List<T> {
        return map.keys.map { get(it)!! }
    }
}

class LazyServiceLookupBlueprint<T : Any>(
    private val map: Map<KClass<out T>, (InjectionContext) -> T>
) {
    fun with(context: InjectionContext) = LazyServiceLookup(context, map)
}
