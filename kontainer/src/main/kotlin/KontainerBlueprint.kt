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
     * A set of all dynamic service definition
     */
    private val dynamics = definitions.filterValues { it.type == InjectionType.Dynamic }

    /**
     * A set of all dynamic service classes
     */
    private val dynamicsClasses = dynamics.keys

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
    private val dependencyLookUp = DependencyLookup(superTypeLookup, definitions.keys)

    /**
     * Collect Prototype services
     */
    private val prototypes: Map<KClass<*>, ServiceProvider.ForPrototype> = definitions
        .filterValues { it.type == InjectionType.Prototype }
        .mapValues { (_, v) -> ServiceProvider.ForPrototype(v) }

    /**
     * Semi dynamic services
     *
     * These are services that where initially defined as singletons, but which inject dynamic services.
     * Or which inject services that themselves inject dynamic services etc...
     */
    private val semiDynamics: Map<KClass<*>, ServiceDefinition> =
        dependencyLookUp.getAllDependents(dynamicsClasses)
            // prototype cannot be "downgraded" to be dynamic singletons
            // TODO: write unit-test that ensures the above statement
            .filter { !prototypes.contains(it) }
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
        .filterKeys { !dynamics.contains(it) }
        .filterKeys { !semiDynamics.contains(it) }
        .filterKeys { !prototypes.contains(it) }
        .mapValues { (_, v) -> ServiceProvider.ForSingleton(ServiceProvider.Type.Singleton, v) }

    /**
     * Extends the current blueprint with everything in [builder] and returns a new [KontainerBlueprint]
     *
     * TODO: test me
     */
    fun extend(builder: KontainerBuilder.() -> Unit): KontainerBlueprint {

        val result = KontainerBuilder(builder).buildBlueprint()

        return KontainerBlueprint(
            config.plus(result.config),
            definitions.plus(result.definitions),
            definitionLocations.plus(result.definitionLocations)
        )
    }

    /**
     * Creates a kontainer instance with the given dynamic services.
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
                    ServiceProvider.Type.Dynamic,
                    it
                )
            }
        )
    }

    /**
     * Creates a new kontainer
     */
    private fun instantiate(overwrittenDynamics: List<Pair<KClass<*>, ServiceProvider>>): Kontainer {

        return Kontainer(
            this,
            // all singletons
            singletons
                // add providers for prototype services
                .plus(prototypes)
                // create new providers for all semi dynamic services
                .plus(semiDynamics.map { (k, v) ->
                    k to ServiceProvider.ForSingleton(ServiceProvider.Type.SemiDynamic, v)
                })
                // create new providers for all dynamic services
                .plus(dynamics.map { (k, v) ->
                    k to ServiceProvider.ForSingleton(ServiceProvider.Type.DynamicDefault, v)
                })
                // add providers for all overwritten dynamic services
                .plus(overwrittenDynamics)

        ).apply {
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
                "${serviceIdx + 1}. Service '${cls.qualifiedName}' " +
                        "(defined at ${definitionLocations[cls] ?: "n/a"})\n" +
                        errors.joinToString("\n") { "    -> $it" }
            }

        if (errors.isNotEmpty()) {

            val err = "Kontainer is inconsistent!\n\n" +
                    "Problems:\n\n" +
                    errors.joinToString("\n") + "\n\n" +
                    "Config values:\n\n" +
                    config.map { (k, v) -> "${k.padEnd(10)} => '$v' (${v::class.qualifiedName})" }.joinToString("\n") + "\n"

            throw KontainerInconsistent(err)
        }
    }
}
