package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * A lookup for mapping service class to service classes that they request for injection.
 */
class DependencyLookup internal constructor(
    superTypeLookUp: TypeLookup.ForSuperTypes,
    definitions: Map<KClass<*>, ServiceDefinition>
) {

    /**
     * The lookup map
     *
     * It is built by the init method.
     *
     * Map Key:   A service class that other services might inject
     * Map Value: A set of services class that directly inject the key service
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
        val record = { def: ServiceDefinition ->

            def.producer.signature.forEach { parameter ->

                val type = parameter.type
                val paramCls = type.classifier as KClass<*>

                val candidates = when {
                    // lazy list of types
                    type.isLazyListType() -> superTypeLookUp.getAllCandidatesFor(type.getInnerInnerClass())

                    // list of lazy types
                    type.isListType() || type.isLazyServiceType() || type.isLookupType() ->
                        superTypeLookUp.getAllCandidatesFor(type.getInnerClass())

                    // default service type
                    paramCls.isServiceType() -> superTypeLookUp.getAllCandidatesFor(paramCls)

                    else -> setOf()
                }

                candidates.forEach { superType ->
                    tmp.add(superType, def.serviceCls)
                }
            }
        }

        definitions.values.forEach { record(it) }

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
