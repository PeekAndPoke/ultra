package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The Kontainer blueprint is built by the [KontainerBuilder]
 */
class KontainerBlueprint internal constructor(
    val config: Config,
    internal val configValues: Map<String, Any>,
    private val definitions: Map<KClass<*>, ServiceDefinition>,
    private val definitionLocations: Map<KClass<*>, StackTraceElement>
) {
    data class Config(
        val trackKontainers: Boolean = false
    ) {
        companion object {
            val default = Config()
        }
    }

    val tracker = when (config.trackKontainers) {
        true -> KontainerTracker.live()
        else -> KontainerTracker.dummy()
    }

    /**
     * Counts how often times the blueprint was used
     */
    private var usages = 0

    /**
     * Collect dynamic service definitions
     */
    internal val dynamicDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.type == InjectionType.Dynamic }

    /**
     * Collect Prototype service definitions
     */
    internal val prototypeDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.type == InjectionType.Prototype }

    /**
     * A set of all dynamic service classes
     */
    private val dynamicsClasses = dynamicDefinitions.keys

    /**
     * A lookup for finding the base types of dynamic services from given super types
     */
    internal val dynamicsBaseTypeLookUp = TypeLookup.ForBaseTypes(dynamicsClasses)

    /**
     * Used to check whether unexpected instances are passed to [create]
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
    internal val semiDynamicDefinitions: Map<KClass<*>, ServiceDefinition> =
        // prototype cannot be "downgraded" to be semi dynamic singletons
        dependencyLookUp.getAllDependents(dynamicsClasses)
            // prototype cannot be "downgraded" to be semi dynamic singletons
            .filter { !prototypeDefinitions.contains(it) }
            .associateWith { definitions.getValue(it) }

    /**
     * Collect Singletons services/
     *
     * These are the services that
     * - have no transitive dependency to any of the dynamic services
     * - are no prototype services
     */
    internal val singletonDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterKeys { !dynamicDefinitions.contains(it) }
        .filterKeys { !semiDynamicDefinitions.contains(it) }
        .filterKeys { !prototypeDefinitions.contains(it) }


    internal val serviceProviderProviders = mapOf<KClass<*>, ServiceProvider.Provider>()
        .plus(
            singletonDefinitions.mapValues { (_, v) ->
                ServiceProvider.ForGlobalSingleton.Provider(ServiceProvider.Type.Singleton, v)
            }
        ).plus(
            prototypeDefinitions.mapValues { (_, v) ->
                ServiceProvider.ForPrototype.Provider(v)
            }
        ).plus(
            semiDynamicDefinitions.mapValues { (_, v) ->
                ServiceProvider.ForDynamicSingleton.Provider(ServiceProvider.Type.SemiDynamic, v)
            }
        ).plus(
            dynamicDefinitions.mapValues { (_, v) ->
                ServiceProvider.ForDynamicSingleton.Provider(ServiceProvider.Type.Dynamic, v)
            }
        )

    /**
     * Extends the current blueprint with everything in [builder] and returns a new [KontainerBlueprint]
     *
     * TODO: test me
     */
    fun extend(builder: KontainerBuilder.() -> Unit): KontainerBlueprint {

        val result = KontainerBuilder(builder).build()

        return KontainerBlueprint(
            config = config,
            configValues = configValues.plus(result.configValues),
            definitions = definitions.plus(result.definitions),
            definitionLocations = definitionLocations.plus(result.definitionLocations)
        )
    }

    /**
     * Creates a kontainer instance and overrides the given dynamic services.
     *
     * The parameter [dynamics] can set dynamic overrides
     *
     * @throws KontainerInconsistent when there is a problem with the kontainer configuration
     */
    fun create(dynamics: DynamicOverrides.Builder.() -> Unit = {}): Kontainer {

        // On the first usage we validate the consistency of the container
        if (usages++ == 0) {
            // Validate the overall consistency
            validate()
        }

        val dynamicServices = DynamicOverrides.Builder().apply(dynamics).build()

        val unexpectedDynamics = dynamicsChecker.getUnexpected(dynamicServices.overrides.keys)

        if (unexpectedDynamics.isNotEmpty()) {
            throw KontainerInconsistent(
                "Unexpected dynamics were provided: " +
                        unexpectedDynamics.map { it.qualifiedName }.joinToString(", ")
            )
        }

        return instantiate(dynamicServices)
    }

    /**
     * Creates a new kontainer
     */
    internal fun instantiate(dynamics: DynamicOverrides): Kontainer {

        val factory = ServiceProviderFactory(
            blueprint = this,
            providerProviders = serviceProviderProviders,
            dynamics = dynamics,
        )

        return Kontainer(factory).apply {
            // Track the Kontainer
            tracker.track(this)
        }
    }

    private fun validate() {

        // Create a container with NO overwritten dynamic services to check to overall consistency
        val container = instantiate(DynamicOverrides(emptyMap()))

        // Validate all service providers are consistent
        val errors = container.getFactory().getAllProviders()
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
                    configValues.map { (k, v) ->
                        "${k.padEnd(10)} => '$v' (${v::class.qualifiedName})"
                    }.joinToString("\n") + "\n"

            throw KontainerInconsistent(err)
        }
    }
}
