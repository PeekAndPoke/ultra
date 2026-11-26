package io.peekandpoke.ultra.kontainer

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Helper class to validate the correctness service instances passed to [KontainerBlueprint.create] at runtime.
 *
 * For each set of classes passed to [getUnexpected] the result is cached for future reuse.
 */
class DynamicsChecker internal constructor(val dynamics: Set<KClass<*>>) {

    /**
     * Internal cache
     *
     * A set of classes will have the same hash for the same classes, so we can do some caching here
     */
    private val unexpectedLookUp = ConcurrentHashMap<Set<KClass<*>>, Set<KClass<*>>>()

    /**
     * Checks if all [given] are contained in all [dynamics]
     *
     * If all is well an empty set is returned.
     * Otherwise, the set will contain all classes that are not expected.
     */
    fun getUnexpected(given: Set<KClass<*>>): Set<KClass<*>> {

        // Stored under a copy: [given] may be a live view of a request's overrides, which would keep them alive
        return unexpectedLookUp[given] ?: unexpectedLookUp.getOrPut(given.toSet()) {
            given.filter { givenClass ->
                dynamics.none { dynamic -> dynamic.java.isAssignableFrom(givenClass.java) }
            }.toSet()
        }
    }
}
