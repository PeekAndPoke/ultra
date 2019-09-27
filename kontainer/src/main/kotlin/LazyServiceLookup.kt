package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

class LazyServiceLookup<T : Any>(

    private val kontainer: Kontainer,
    private val map: Map<KClass<out T>, (Kontainer) -> T>

) : Lookup<T> {

    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> get(cls: KClass<X>): X? = map[cls]?.invoke(kontainer) as X?

    override fun all(): List<T> {
        return map.keys.map { get(it)!! }
    }
}

class LazyServiceLookupBlueprint<T : Any>(
    private val map: Map<KClass<out T>, (Kontainer) -> T>
) {
    fun with(kontainer: Kontainer) = LazyServiceLookup(kontainer, map)
}
