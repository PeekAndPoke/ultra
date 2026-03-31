package io.peekandpoke.ultra.kontainer

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Helpers for looking up base and super types.
 */
abstract class TypeLookup {

    /**
     * Get all matching types for the given [type]
     */
    abstract fun getAllCandidatesFor(type: KClass<*>): Set<KClass<*>>

    /**
     * Gets a distinct candidate for the given [type]
     *
     * If there is no super type or more than one found, a [ServiceNotFound] is thrown.
     */
    fun getDistinctFor(type: KClass<*>): KClass<*> {

        val candidates = getAllCandidatesFor(type)

        if (candidates.isEmpty()) {
            throw ServiceNotFound("Service ${type.getName()} was not found")
        }

        if (candidates.size > 1) {
            throw ServiceAmbiguous(
                "Service ${type.getName()} is ambiguous. It has multiple candidates: " +
                        candidates.joinToString(", ") { it.getName() }
            )
        }

        return candidates.first()
    }

    /**
     * Helper class for finding [baseTypes] of a given super type
     *
     * Result to calls of [getAllCandidatesFor] and cached internally for future reuse.
     */
    data class ForBaseTypes(val baseTypes: Set<KClass<*>>) : TypeLookup() {

        /**
         * Internal cache map from classes to their base types
         */
        private val cache = ConcurrentHashMap<KClass<*>, Set<KClass<*>>>()

        /**
         * Gets all [baseTypes] that are base types of the given super [type]
         */
        override fun getAllCandidatesFor(type: KClass<*>): Set<KClass<*>> =
            cache.getOrPut(type) {
                baseTypes.filter { baseType ->
                    baseType.java.isAssignableFrom(type.java)
                }.toSet()
            }
    }

    /**
     * Helper class for finding [superTypes] of given base type
     *
     * Result to calls of [getAllCandidatesFor] and cached internally for future reuse.
     */
    data class ForSuperTypes(val superTypes: Set<KClass<*>>) : TypeLookup() {

        /**
         * Internal cache map from classes to their super types
         */
        private val candidatesCache = ConcurrentHashMap<KClass<*>, Set<KClass<*>>>()

        /**
         * Internal cache map from classes to [LazyServiceLookupBlueprint]
         */
        private val lookupBlueprintCache = ConcurrentHashMap<KClass<*>, LazyServiceLookupBlueprint<*>>()

        /**
         * Gets all [superTypes] for the given base [type]
         */
        override fun getAllCandidatesFor(type: KClass<*>): Set<KClass<*>> =
            candidatesCache.getOrPut(type) {
                superTypes
                    .filter { superType -> type.java.isAssignableFrom(superType.java) }
                    .toSet()
            }

        /**
         * Get all [superTypes] for the given base [type] as a [LazyServiceLookupBlueprint]
         */
        fun getLookupBlueprint(type: KClass<*>): LazyServiceLookupBlueprint<*> =
            lookupBlueprintCache.getOrPut(type) {

                val map = getAllCandidatesFor(type)
                    .associateWith {
                        { kontainer: Kontainer, context: InjectionContext ->
                            kontainer.get(it, context)
                        }
                    }

                LazyServiceLookupBlueprint(map)
            }

        /**
         * Get a distinct super type of the given [type] or null if there is none or multiple candidates
         */
        fun <T : Any> getDistinctForOrNull(type: KClass<T>): KClass<T>? {
            val candidates = getAllCandidatesFor(type)

            @Suppress("UNCHECKED_CAST")
            return when (candidates.size) {
                1 -> candidates.first() as KClass<T>
                else -> null
            }
        }
    }
}
