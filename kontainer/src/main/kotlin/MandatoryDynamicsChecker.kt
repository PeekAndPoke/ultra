package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Helper class to validate the all [mandatory] dynamic services are provided to [validate].
 *
 * For each set of classes passed to [validate] the result is cached for future reuse.
 */
data class MandatoryDynamicsChecker(val mandatory: Set<KClass<*>>) {

    /**
     * Internal cache
     *
     * A set of classes will have the same hash for the same classes, so we can do some caching here
     */
    private val lookUp = mutableMapOf<Set<KClass<*>>, List<KClass<*>>>()

    /**
     * Checks if [given] contains all [mandatory]
     *
     * If all is well an empty list is returned.
     * Otherwise the list will contain all classes that are missing.
     */
    fun validate(given: Set<KClass<*>>): List<KClass<*>> {

        return lookUp.getOrPut(given) {
            mandatory.filter { mandatoryClass ->
                given.none { givenClass ->
                    mandatoryClass.java.isAssignableFrom(givenClass.java)
                }
            }
        }
    }
}
