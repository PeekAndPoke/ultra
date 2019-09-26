package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

class LazyServiceLookup<T : Any>(

    private val kontainer: Kontainer,
    private val map: Map<KClass<out T>, () -> T>

) : Lookup<T> {

    private val internalLookup = mutableMapOf<KClass<out T>, T>()

    fun reset(kontainer: Kontainer) = LazyServiceLookup(kontainer, map)

    override fun <X : T> has(cls: KClass<X>): Boolean = map.contains(cls)

    @Suppress("UNCHECKED_CAST")
    override fun <X : T> get(cls: KClass<X>): X = internalLookup.getOrPut(cls) {
        map.getValue(cls).invoke()
    } as X

    override fun all(): List<T> {
        return map.keys.map { get(it) }
    }
}
