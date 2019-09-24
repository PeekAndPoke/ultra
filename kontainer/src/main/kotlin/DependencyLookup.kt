package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * A lookup for finding out which classes are injected into which
 */
class DependencyLookup(classes: Set<KClass<*>>) {

    /**
     * The lookup map built by the init method
     */
    private val dependencies: Map<KClass<*>, Set<KClass<*>>>

    init {
        /**
         * Temporary map for building the lookup
         */
        val tmp = mutableMapOf<KClass<*>, MutableSet<KClass<*>>>()

        /**
         * Records the injected types of the given cls
         */
        val record = { cls: KClass<*> ->
            if (cls.primaryConstructor != null) {
                cls.primaryConstructor!!.parameters.forEach {
                    tmp.getOrPut(it.type.classifier as KClass<*>) { mutableSetOf() }.add(cls)
                }
            }
        }

        classes.forEach { record(it) }

        dependencies = tmp
    }

    /**
     * Return a set service classes that directly or indirectly depend on one of the classes given
     */
    fun getAllDependents(of: Set<KClass<*>>): Set<KClass<*>> {

        var current = of
        var previous = setOf<KClass<*>>()

        // Not the best/fastest implementation ...
        while (current.size > previous.size) {

            previous = current

            val found = current.flatMap { dependencies[it] ?: setOf() }

            current = current.plus(found)
        }

        // NOTICE: we remove the initial set [of] as we really only want to know their dependents
        return current.minus(of)
    }
}
