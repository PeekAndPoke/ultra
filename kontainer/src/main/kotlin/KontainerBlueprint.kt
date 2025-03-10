package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The Kontainer blueprint is built by the [KontainerBuilder]
 */
class KontainerBlueprint internal constructor(
    val config: Config,
    internal val definitions: Map<KClass<*>, ServiceDefinition>,
) {
    data class Config(
        val trackKontainers: Boolean = false,
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
    private val dynamicDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.injectionType == InjectionType.Dynamic }

    /**
     * Collect Prototype service definitions
     */
    private val prototypeDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterValues { it.injectionType == InjectionType.Prototype }

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
    @PublishedApi
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
    private val singletonDefinitions: Map<KClass<*>, ServiceDefinition> = definitions
        .filterKeys { !dynamicDefinitions.contains(it) }
        .filterKeys { !semiDynamicDefinitions.contains(it) }
        .filterKeys { !prototypeDefinitions.contains(it) }

    /**
     * All precomputed [ServiceProvider.Provider]s
     */
    private val serviceProviderProviders = mapOf<KClass<*>, ServiceProvider.Provider>()
        .plus(
            singletonDefinitions.mapValues { (_, v) ->
                ServiceProvider.Provider.forGlobalSingleton(ServiceProvider.Type.Singleton, v)
            }
        ).plus(
            semiDynamicDefinitions.mapValues { (_, v) ->
                ServiceProvider.Provider.forDynamicSingleton(ServiceProvider.Type.SemiDynamic, v)
            }
        ).plus(
            dynamicDefinitions.mapValues { (_, v) ->
                ServiceProvider.Provider.forDynamicSingleton(ServiceProvider.Type.Dynamic, v)
            }
        ).plus(
            prototypeDefinitions.mapValues { (_, v) ->
                ServiceProvider.ForPrototype.Provider(v)
            }
        )

    /**
     * Extends the current blueprint with everything in [builder] and returns a new [KontainerBlueprint]
     */
    fun extend(builder: KontainerBuilder.() -> Unit): KontainerBlueprint {

        val result = KontainerBuilder(builder).build()

        return KontainerBlueprint(
            config = config,
            definitions = definitions.plus(result.definitions),
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
                "Unexpected dynamics were provided: " + unexpectedDynamics.joinToString(", ") { it.getName() }
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
        val errors = container.getServiceProviderFactory().getAllProviders()
            .mapValues { (_, provider) -> provider to provider.validate(container) }
            .filterValues { (_, errors) -> errors.isNotEmpty() }
            .toList()
            .mapIndexed { serviceIdx, (cls, entry) ->

                val (provider, errors) = entry
                val codeLocation = provider.definition.codeLocation.first()
                val name = cls.getName()

                "${serviceIdx + 1}. Service '${name}'\n" +
                        "    defined at ${codeLocation ?: "n/a"})\n" +
                        errors.joinToString("\n") { "    -> $it" }
            }

        if (errors.isNotEmpty()) {
            val err = "Kontainer is inconsistent!\n\n" +
                    "Problems:\n\n" +
                    errors.joinToString("\n") + "\n\n"

            throw KontainerInconsistent(err)
        }
    }
}
