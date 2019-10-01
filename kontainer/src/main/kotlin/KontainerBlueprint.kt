package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

data class KontainerBlueprint internal constructor(
    val config: Map<String, Any>,
    val definitions: Map<KClass<*>, ServiceDefinition>,
    val definitionLocations: Map<KClass<*>, StackTraceElement>
) {

    /**
     * Counts how often times the blueprint was used
     */
    private var usages = 0

    /**
     * A set of all dynamic services
     */
    private val dynamics = definitions.filterValues { it.type == InjectionType.Dynamic }.keys

    /**
     * A set of all dynamics that have a default value
     */
    private val optionalDynamics = definitions.filterValues {
        // optional dynamic services do have a default provided (hence a producer is present)
        it.type == InjectionType.Dynamic && it.producer != null
    }

    /**
     * A set of all services which need to be passed to [useWith]
     */
    private val mandatoryDynamics = definitions.filterValues {
        // mandatory dynamic services do not have a default provided (hence no producer)
        it.type == InjectionType.Dynamic && it.producer == null
    }.keys

    /**
     * A lookup for finding the base types of dynamic services from given super types
     */
    private val dynamicsBaseTypeLookUp = TypeLookup.ForBaseTypes(dynamics)

    /**
     * Used to check whether all mandatory services are passed to [useWith]
     */
    private val mandatoryDynamicsChecker = MandatoryDynamicsChecker(mandatoryDynamics, dynamics)

    /**
     * Base type lookup for finding all candidate services by a given super type
     */
    private val superTypeLookup = TypeLookup.ForSuperTypes(definitions.keys)

    /**
     * Dependency lookup for figuring out which service depends on which other services
     */
    private val dependencyLookUp = DependencyLookup(superTypeLookup, definitions.keys)

    /**
     * Semi dynamic services
     *
     * These are services that where initially defined as singletons, but which inject dynamic services.
     * Or which inject services that themselves inject dynamic services etc...
     */
    private val semiDynamics: Map<KClass<*>, ServiceDefinition> =
        dependencyLookUp.getAllDependents(dynamics).map {
            it to definitions.getValue(it)
        }.toMap()

    /**
     * Collect Prototype services
     */
    private val prototypes: Map<KClass<*>, ServiceProvider.ForPrototype> = definitions
        .filterValues { it.type == InjectionType.Prototype }
        .mapValues { (_, v) -> ServiceProvider.ForPrototype.of(v) }

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
        .mapValues { (_, v) -> ServiceProvider.ForSingleton.of(ServiceProvider.Type.Singleton, v) }

    /**
     * Creates a kontainer instance with the given dynamic services.
     */
    fun useWith(vararg dynamics: Any): Kontainer {

        // On the first usage we validate the consistency of the container
        if (usages++ == 0) {
            validate()
        }

        val dynamicClasses = dynamics.map { it::class }.toSet()

        val missingDynamics = mandatoryDynamicsChecker.getMissing(dynamicClasses)

        if (missingDynamics.isNotEmpty()) {
            throw KontainerInconsistent(
                "Some dynamics were not provided: " +
                        missingDynamics.map { it.qualifiedName }.joinToString(", ")
            )
        }

        val unexpectedDynamics = mandatoryDynamicsChecker.getUnexpected(dynamicClasses)

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
    private fun instantiate(dynamicsProviders: List<Pair<KClass<*>, ServiceProvider>>): Kontainer {
        return Kontainer(
            superTypeLookup,
            config,
            // all singletons
            singletons
                // add providers for prototype services
                .plus(prototypes)
                // create new providers for all semi dynamic services
                .plus(semiDynamics.map { (k, v) ->
                    k to ServiceProvider.ForSingleton.of(ServiceProvider.Type.SemiDynamic, v)
                })
                // create new providers for all optional dynamic services
                .plus(optionalDynamics.map { (k, v) ->
                    k to ServiceProvider.ForSingleton.of(ServiceProvider.Type.DynamicDefault, v)
                })
                // add providers for dynamic services
                .plus(dynamicsProviders)
        )
    }

    private fun validate() {

        // create a container with dummy entries for the mandatory dynamic services
        val container = instantiate(
            mandatoryDynamics.map { it to ServiceProvider.ForInstance(ServiceProvider.Type.Dynamic, it) }
        )

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
