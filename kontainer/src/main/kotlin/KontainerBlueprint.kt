package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The Kontainer blueprint is built by the [KontainerBuilder]
 */
class KontainerBlueprint internal constructor(
    internal val config: Map<String, Any>,
    private val definitions: Map<KClass<*>, ServiceDefinition>,
    private val definitionLocations: Map<KClass<*>, StackTraceElement>
) {

    val tracker = KontainerTracker()

    /**
     * Counts how often times the blueprint was used
     */
    private var usages = 0

    /**
     * Collect dynamic service definitions
     */
    private val dynamicDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.type == InjectionType.Dynamic }

    /**
     * Collect Prototype service definitions
     */
    private val prototypeDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.type == InjectionType.Prototype }

    /**
     * A set of all dynamic service classes
     */
    private val dynamicsClasses = dynamicDefinitions.keys

    /**
     * A lookup for finding the base types of dynamic services from given super types
     */
    private val dynamicsBaseTypeLookUp = TypeLookup.ForBaseTypes(dynamicsClasses)

    /**
     * Used to check whether unexpected instances are passed to [useWith]
     */
    private val dynamicsChecker = DynamicsChecker(dynamicsClasses)

    /**
     * Base type lookup for finding all candidate services by a given super type
     */
    internal val superTypeLookup = TypeLookup.ForSuperTypes(definitions.keys)

    /**
     * Dependency lookup for figuring out which service depends on which other services
     */
    private val dependencyLookUp = DependencyLookup(superTypeLookup, definitions)

    /**
     * Semi dynamic services
     *
     * These are services that where initially defined as singletons, but which inject dynamic services.
     * Or which inject services that themselves inject dynamic services etc...
     */
    private val semiDynamicDefinitions: Map<KClass<*>, ServiceDefinition> =
        dependencyLookUp.getAllDependents(dynamicsClasses)
            // prototype cannot be "downgraded" to be semi dynamic singletons
            .filter { !prototypeDefinitions.contains(it) }
            .map { it to definitions.getValue(it) }
            .toMap()

    /**
     * Collect Singletons services/
     *
     * These are the services that
     * - have no transitive dependency to any of the dynamic services
     * - are no prototype services
     */
    private val singletons: Map<KClass<*>, ServiceProvider.ForSingleton> = definitions
        .filterKeys { !dynamicDefinitions.contains(it) }
        .filterKeys { !semiDynamicDefinitions.contains(it) }
        .filterKeys { !prototypeDefinitions.contains(it) }
        .mapValues { (_, v) -> ServiceProvider.ForSingleton(ServiceProvider.Type.Singleton, v) }

    /**
     * Extends the current blueprint with everything in [builder] and returns a new [KontainerBlueprint]
     *
     * TODO: test me
     */
    fun extend(builder: KontainerBuilder.() -> Unit): KontainerBlueprint {

        val result = KontainerBuilder(builder).build()

        return KontainerBlueprint(
            config.plus(result.config),
            definitions.plus(result.definitions),
            definitionLocations.plus(result.definitionLocations)
        )
    }

    /**
     * Creates a kontainer instance without overriding any of the dynamic services.
     *
     * @throws KontainerInconsistent when there is a problem with the kontainer configuration
     */
    fun create() = useWith()

    /**
     * Creates a kontainer instance and overrides the given dynamic services.
     *
     * @throws KontainerInconsistent when there is a problem with the kontainer configuration
     */
    fun useWith(vararg dynamics: Any): Kontainer {

        // On the first usage we validate the consistency of the container
        if (usages++ == 0) {
            validate()
        }

        val givenClasses = dynamics.map { it::class }.toSet()

        val unexpectedDynamics = dynamicsChecker.getUnexpected(givenClasses)

        if (unexpectedDynamics.isNotEmpty()) {
            throw KontainerInconsistent(
                "Unexpected dynamics were provided: " +
                        unexpectedDynamics.map { it.qualifiedName }.joinToString(", ")
            )
        }

        return instantiate(
            dynamics.map {
                dynamicsBaseTypeLookUp.getDistinctFor(it::class) to ServiceProvider.ForInstance(
                    ServiceProvider.Type.DynamicOverride,
                    it
                )
            }
        )
    }

    /**
     * Creates a new kontainer
     */
    private fun instantiate(overwrittenDynamics: List<Pair<KClass<*>, ServiceProvider>>): Kontainer {

        val map = HashMap<KClass<*>, ServiceProvider>(
            singletons.size +
                    prototypeDefinitions.size +
                    dynamicDefinitions.size +
                    semiDynamicDefinitions.size +
                    overwrittenDynamics.size
        )

        // put all singletons
        map.putAll(singletons)

        // add providers for prototype services
        map.putAll(prototypeDefinitions.mapValues { (_, v) -> ServiceProvider.ForPrototype(v) })

        // create new providers for all semi dynamic services
        semiDynamicDefinitions
            .forEach { (k, v) -> map[k] = ServiceProvider.ForSingleton(ServiceProvider.Type.SemiDynamic, v) }

        // create new providers for all dynamic services
        dynamicDefinitions
            .forEach { (k, v) -> map[k] = ServiceProvider.ForSingleton(ServiceProvider.Type.Dynamic, v) }

        // add providers for all overwritten dynamic services
        map.putAll(overwrittenDynamics)

        return Kontainer(this, map).apply {
            // Track the Kontainer
            tracker.track(this)
        }
    }

    private fun validate() {

        // create a container with no overwritten dynamic services
        val container = instantiate(listOf())

        // validate all service providers
        val errors = container.providers
            .mapValues { (_, v) -> v.validate(container) }
            .filterValues { it.isNotEmpty() }
            .toList()
            .mapIndexed { serviceIdx, (cls, errors) ->
                "${serviceIdx + 1}. Service '${cls.qualifiedName}'\n" +
                        "    defined at ${definitionLocations[cls] ?: "n/a"})\n" +
                        errors.joinToString("\n") { "    -> $it" }
            }

        if (errors.isNotEmpty()) {

            val err = "Kontainer is inconsistent!\n\n" +
                    "Problems:\n\n" +
                    errors.joinToString("\n") + "\n\n" +
                    "Config values:\n\n" +
                    config.map { (k, v) ->
                        "${k.padEnd(10)} => '$v' (${v::class.qualifiedName})"
                    }.joinToString("\n") + "\n"

            throw KontainerInconsistent(err)
        }
    }
}
