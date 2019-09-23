package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

data class KontainerBlueprint(val config: Map<String, Any>, val classes: Map<KClass<*>, InjectionType>) {

    /**
     * Counts how often times the blueprint was used
     */
    private var usages = 0

    /**
     * A set of all classes that need to passed to [useWith]
     */
    private val mandatoryDynamics = classes.filterValues { it == InjectionType.Dynamic }.keys

    /**
     * A lookup for finding the base types of mandatory dynamic services from given super types
     */
    private val mandatoryDynamicsBaseTypeLookUp = TypeLookup.ForBaseTypes(mandatoryDynamics)

    /**
     * Used to check whether all mandatory services are passed to [useWith]
     */
    private val mandatoryDynamicsChecker = MandatoryDynamicsChecker(mandatoryDynamics)

    /**
     * Dependency lookup for figuring out which service depends on which other services
     */
    private val dependencyLookUp = DependencyLookup(classes.keys)

    /**
     * Base type lookup for finding all candidate services by a given super type
     */
    private val superTypeLookup = TypeLookup.ForSuperTypes(classes.keys)

    /**
     * Semi dynamic services
     *
     * These are services that where initially defined as singletons, but which inject dynamic services.
     * Or which inject services that themselves inject dynamic services etc...
     */
    private val semiDynamics: Set<KClass<*>> = dependencyLookUp.getAllDependents(mandatoryDynamics)

    /**
     * Global services are the services that have no dependency to any of the dynamic services
     */
    private val globalSingletons: Map<KClass<*>, ServiceProvider> = classes
        .filterKeys { !mandatoryDynamics.contains(it) }
        .filterKeys { !semiDynamics.contains(it) }
        .mapValues { (k, _) ->
            ServiceProvider.ForSingleton.of(ServiceProvider.Type.GlobalSingleton, k)
        }

    /**
     * Creates a kontainer instance with the given dynamic services.
     */
    fun useWith(vararg dynamics: Any): Kontainer {

        // On the first usage we validate the consistency of the container
        if (usages++ == 0) {
            validate()
        }

        mandatoryDynamicsChecker.validate(dynamics.map { it::class }.toSet()).apply {
            if (isNotEmpty()) {
                throw KontainerInconsistent(
                    "Cannot create Kontainer! Some mandatory dynamic services are missing: " +
                            map { it.qualifiedName }.joinToString(", ")
                )
            }
        }

        return instantiate(
            dynamics.map {
                mandatoryDynamicsBaseTypeLookUp.getDistinctFor(it::class) to ServiceProvider.ForInstance(
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
            globalSingletons
                // add singleton providers for all semi dynamic services
                .plus(semiDynamics.map {
                    it to ServiceProvider.ForSingleton.of(ServiceProvider.Type.SemiDynamic, it)
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
                "${serviceIdx + 1}. Service '${cls.qualifiedName}'\n" + errors.joinToString("\n") { "    -> $it" }
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
