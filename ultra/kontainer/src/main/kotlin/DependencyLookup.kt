package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * A lookup for mapping service class to service classes that they request for injection.
 *
 * Built only from what each [ParameterProvider] announces: the injected service types and the provision type.
 */
class DependencyLookup internal constructor(
    blueprint: KontainerBlueprint,
    definitions: Map<KClass<*>, ServiceDefinition>
) {

    /**
     * The reverse dependency lookup map (built by init).
     *
     * Map Key:   A service class that other services might inject
     * Map Value: A set of services class that directly inject the key service
     */
    private val dependencies: Map<KClass<*>, Set<KClass<*>>>

    /**
     * Forward dependency map of the Direct injections (for cycle detection).
     *
     * Map Key:   A service class
     * Map Value: The set of service classes it directly depends on, without Lazy injections and without an ambiguous
     *            single-service injection, which the parameter check reports
     */
    private val directDependencies: Map<KClass<*>, Set<KClass<*>>>

    init {
        val tmp = mutableMapOf<KClass<*>, MutableSet<KClass<*>>>()
        val forwardTmp = mutableMapOf<KClass<*>, MutableSet<KClass<*>>>()

        fun MutableMap<KClass<*>, MutableSet<KClass<*>>>.add(master: KClass<*>, dependsOn: KClass<*>) =
            getOrPut(master) { mutableSetOf() }.add(dependsOn)

        definitions.values.forEach { def ->

            def.producer.params.forEach { parameter ->

                // The candidates of each announced type; a List or Lookup announces candidates already
                val candidatesPerType = parameter.getInjectedServiceTypes(blueprint)
                    .map { blueprint.superTypeLookup.getAllCandidatesFor(it) }

                val candidates = candidatesPerType.flatten().toSet()

                candidates.forEach { tmp.add(it, def.serviceCls) }

                // One announced type with several candidates: an ambiguous single-service injection, not a cycle
                val ambiguous = candidatesPerType.size == 1 && candidatesPerType.single().size > 1

                if (parameter.getProvisionType() == ParameterProvider.ProvisionType.Direct && !ambiguous) {
                    candidates.forEach { forwardTmp.add(def.serviceCls, it) }
                }
            }
        }

        dependencies = tmp
        directDependencies = forwardTmp
    }

    /**
     * Detects circular dependencies among non-lazy injections.
     *
     * Returns a list of cycle descriptions, or an empty list if no cycles exist.
     * Lazy injections are excluded since they break cycles by design.
     */
    fun detectCircularDependencies(): List<String> {
        val errors = mutableListOf<String>()
        val visited = mutableSetOf<KClass<*>>()
        val inStack = mutableSetOf<KClass<*>>()

        fun dfs(node: KClass<*>, path: List<KClass<*>>) {
            if (node in inStack) {
                val cycleStart = path.indexOf(node)
                val cycle = path.subList(cycleStart, path.size) + node
                errors.add(
                    "Circular dependency detected: " +
                            cycle.joinToString(" -> ") { it.getName() }
                )
                return
            }
            if (node in visited) return

            visited.add(node)
            inStack.add(node)

            for (dep in directDependencies[node] ?: emptySet()) {
                dfs(dep, path + node)
            }

            inStack.remove(node)
        }

        for (node in directDependencies.keys) {
            dfs(node, emptyList())
        }

        return errors
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
