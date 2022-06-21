package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The blueprint is a pre-stage of the [LazyServiceLookup] and can be re-used with a [InjectionContext]
 */
class LazyServiceLookupBlueprint<T : Any>(
    private val map: Map<KClass<out T>, (Kontainer, InjectionContext) -> T>,
) {
    /**
     * Get the keys
     */
    fun getClasses(): Set<KClass<out T>> = map.keys

    /**
     * Creates a [LazyServiceLookup] for the given [context]
     */
    fun with(kontainer: Kontainer, context: InjectionContext): LazyServiceLookup<T> {
        return LazyServiceLookup(kontainer, context, map)
    }
}
