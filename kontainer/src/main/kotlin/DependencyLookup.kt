package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * A lookup for finding out which classes are injected into which
 */
class DependencyLookup internal constructor(
    superTypeLookUp: TypeLookup.ForSuperTypes,
    classes: Set<KClass<*>>
) {

    /**
     * The lookup map built by the init method
     *
     * Map Key:   A service that other services might inject
     * Map Value: A set of services that directly inject the key service
     */
    private val dependencies: Map<KClass<*>, Set<KClass<*>>>

    init {
        /**
         * Temporary map for building the lookup
         */
        val tmp = mutableMapOf<KClass<*>, MutableSet<KClass<*>>>()

        fun MutableMap<KClass<*>, MutableSet<KClass<*>>>.add(master: KClass<*>, dependsOn: KClass<*>) =
            getOrPut(master) { mutableSetOf() }.add(dependsOn)

        /**
         * Records the injected types of the given cls
         */
        val record = { cls: KClass<*> ->
            if (cls.primaryConstructor != null) {
                cls.primaryConstructor!!.parameters.forEach { parameter ->

                    val paramCls = parameter.type.classifier as KClass<*>

                    val candidates = when {
                        // default service type
                        isServiceType(paramCls) -> superTypeLookUp.getAllCandidatesFor(paramCls)

                        // list of lazy types
                        isListType(parameter.type) || isLazyServiceType(parameter.type) -> {
                            val inner = getInnerClass(parameter.type)

                            superTypeLookUp.getAllCandidatesFor(inner)
                        }

                        // lazy list of types
                        isLazyListType(parameter.type) -> {
                            val inner = getInnerInnerClass(parameter.type)

                            superTypeLookUp.getAllCandidatesFor(inner)
                        }

                        else -> setOf()
                    }

                    candidates.forEach { superType ->
                        tmp.add(superType, cls)
                    }
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
