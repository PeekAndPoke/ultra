package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The Kontainer blueprint is built by the [KontainerBuilder]
 */
class KontainerBlueprint internal constructor(
    val config: Config,
    internal val definitions: Map<KClass<*>, ServiceDefinition>,
) {
    /** Configuration options for the blueprint */
    data class Config(
        val trackKontainers: Boolean = false,
    ) {
        companion object {
            val default = Config()
        }
    }

    /** Tracker for monitoring live [Kontainer] instances created from this blueprint */
    val tracker = when (config.trackKontainers) {
        true -> KontainerTracker.live()
        else -> KontainerTracker.dummy()
    }

    /**
     * The validation result, computed once on first use: the error message, or null when the blueprint is valid.
     *
     * A caller arriving while another one validates waits for that result.
     */
    private val validationError: Lazy<String?> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            validate()
            null
        } catch (e: KontainerInconsistent) {
            e.message ?: "Kontainer is inconsistent!"
        }
    }

    /**
     * Services declared dynamic, singletons and prototypes alike.
     *
     * Everything that reaches one of them through its injections is bound to one kontainer.
     */
    private val dynamicClasses: Set<KClass<*>> = definitions
        .filterValues { it.kind == ServiceKind.Dynamic }
        .keys

    /**
     * Dynamic singletons, the built-in services excepted: the only services that can be overridden at [create]
     *
     * `Kontainer` and `InjectionContext` stay dynamic sources of promotion, see [dynamicClasses].
     */
    private val overridableClasses: Set<KClass<*>> = definitions
        .filterValues { it.type == ServiceType.Singleton && it.kind == ServiceKind.Dynamic }
        .keys
        .minus(builtInServices)

    /**
     * A lookup for finding the overridable service from the class of an override
     */
    internal val dynamicsBaseTypeLookUp = TypeLookup.ForBaseTypes(overridableClasses)

    /**
     * Used to check whether unexpected instances are passed to [create]
     */
    private val dynamicsChecker = DynamicsChecker(overridableClasses)

    /**
     * Base type lookup for finding all candidate services by a given super type
     */
    @PublishedApi
    internal val superTypeLookup = TypeLookup.ForSuperTypes(definitions.keys)

    /**
     * Dependency lookup for figuring out which service depends on which other services.
     *
     * Reads the parameters' announcements, which use only [superTypeLookup], initialized above.
     */
    private val dependencyLookUp = DependencyLookup(blueprint = this, definitions = definitions)

    /**
     * Services declared static that reach a dynamic service, directly or through any chain; in discovery order.
     *
     * Declared static by construction: [dynamicClasses] holds every service declared dynamic, and
     * [DependencyLookup.getAllDependents] leaves out the set it starts from.
     */
    private val semiDynamicClasses: Set<KClass<*>> = dependencyLookUp.getAllDependents(dynamicClasses)

    /**
     * The derived kind of every service: as declared, or [ServiceKind.SemiDynamic] for a static one that reaches
     * a dynamic one.
     */
    private val kinds: Map<KClass<*>, ServiceKind> = definitions.mapValues { (cls, def) ->
        when {
            def.kind == ServiceKind.Dynamic -> ServiceKind.Dynamic
            cls in semiDynamicClasses -> ServiceKind.SemiDynamic
            else -> ServiceKind.Static
        }
    }

    /**
     * All precomputed [ServiceProvider.Provider]s.
     *
     * Listed in four groups: static singletons, semi dynamic singletons, dynamic singletons, prototypes of every
     * kind. This order numbers the validation errors and orders the debug output. Semi dynamic singletons come in
     * discovery order, all others in definition order.
     */
    private val serviceProviderProviders: Map<KClass<*>, ServiceProvider.Provider> = run {

        fun ofType(type: ServiceType) = definitions.keys.filter { definitions.getValue(it).type == type }

        val singletons = ofType(ServiceType.Singleton)

        val listed = singletons.filter { kinds.getValue(it) == ServiceKind.Static } +
                semiDynamicClasses.filter { definitions.getValue(it).type == ServiceType.Singleton } +
                singletons.filter { kinds.getValue(it) == ServiceKind.Dynamic } +
                ofType(ServiceType.Prototype)

        listed.associateWith { cls -> providerFor(definitions.getValue(cls), kinds.getValue(cls)) }
    }

    /**
     * The declared dynamic services each semi dynamic service reaches, in definition order.
     */
    private val semiDynamicSources: Map<KClass<*>, List<KClass<*>>> by lazy {
        val sources = mutableMapOf<KClass<*>, MutableList<KClass<*>>>()

        dynamicClasses.forEach { source ->
            dependencyLookUp.getAllDependents(setOf(source))
                .filter { kinds[it] == ServiceKind.SemiDynamic }
                .forEach { sources.getOrPut(it) { mutableListOf() }.add(source) }
        }

        sources
    }

    /**
     * The declared dynamic services that make [cls] semi dynamic; empty when it is not.
     */
    internal fun semiDynamicBecauseOf(cls: KClass<*>): List<KClass<*>> = semiDynamicSources[cls] ?: emptyList()

    /**
     * Chooses the provider by the type first: a prototype is created per injection whatever its kind, so it can
     * never end up as a singleton of any kind.
     */
    private fun providerFor(def: ServiceDefinition, kind: ServiceKind): ServiceProvider.Provider = when (def.type) {
        ServiceType.Prototype -> ServiceProvider.ForPrototype.Provider(kind = kind, definition = def)

        ServiceType.Singleton -> when (kind) {
            ServiceKind.Static -> ServiceProvider.Provider.forGlobalSingleton(definition = def)
            ServiceKind.Dynamic, ServiceKind.SemiDynamic ->
                ServiceProvider.Provider.forDynamicSingleton(kind = kind, definition = def)
        }
    }

    /**
     * Extends the current blueprint with everything in [builder] and returns a new [KontainerBlueprint]
     */
    fun extend(builder: KontainerBuilder.() -> Unit): KontainerBlueprint {

        val result = KontainerBuilder(builder).build()

        // The same redefinition rule as within one builder; it is associative, so the order of application is free
        val upgrade = KontainerBuilder.RedefinitionUpgrade()

        return KontainerBlueprint(
            config = config,
            definitions = definitions.plus(
                result.definitions.mapValues { (cls, def) ->
                    upgrade.adjust(newDef = def, existingDef = definitions[cls])
                }
            ),
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

        // Validation comes before the caller's override block runs, as it always did
        validateOnFirstUse()

        return createChecked(DynamicOverrides.Builder().apply(dynamics).build())
    }

    /**
     * Validates the blueprint on its first use and fails every use of an invalid one with the same message.
     */
    private fun validateOnFirstUse() {
        validationError.value?.let { throw KontainerInconsistent(it) }
    }

    /**
     * The one entry point of [create] and [Kontainer.clone]: validates on first use, checks the [dynamics] and
     * creates the kontainer.
     *
     * @throws KontainerInconsistent when there is a problem with the kontainer configuration or the overrides
     */
    internal fun createChecked(dynamics: DynamicOverrides): Kontainer {

        validateOnFirstUse()

        val overridden = dynamics.overrides.keys

        overridden.firstOrNull { it in builtInServices }?.let { builtIn ->
            throw KontainerInconsistent(
                "${builtIn.getName()} is managed by the kontainer itself and cannot be overridden"
            )
        }

        val unexpectedDynamics = dynamicsChecker.getUnexpected(overridden)

        if (unexpectedDynamics.isNotEmpty()) {
            throw KontainerInconsistent(
                "Unexpected dynamics were provided: " + unexpectedDynamics.joinToString(", ") { it.getName() } +
                        dynamicPrototypeHint(unexpectedDynamics)
            )
        }

        // Every override resolves to one service now, so an ambiguous one is refused before any override lambda runs
        overridden.forEach { overriddenServiceOf(it) }

        return instantiate(dynamics)
    }

    /**
     * The service an override given as [cls] replaces: [cls] itself when it is an overridable dynamic, otherwise the
     * one overridable dynamic it is assignable to.
     *
     * Exact membership uses class equality, as the definitions do; only the fallback asks Java assignability.
     *
     * @throws ServiceAmbiguous when [cls] is not overridable itself and fits several
     */
    internal fun overriddenServiceOf(cls: KClass<*>): KClass<*> = when (cls) {
        in overridableClasses -> cls
        else -> dynamicsBaseTypeLookUp.getDistinctFor(cls)
    }

    /**
     * Explains a refused override of a dynamic prototype; empty when none of [unexpected] is one.
     */
    private fun dynamicPrototypeHint(unexpected: Set<KClass<*>>): String {

        val dynamicPrototypes = definitions
            .filterValues { it.type == ServiceType.Prototype && it.kind == ServiceKind.Dynamic }
            .keys
            .filter { dynamicPrototype -> unexpected.any { dynamicPrototype.java.isAssignableFrom(it.java) } }

        return when {
            dynamicPrototypes.isEmpty() -> ""
            else -> " (only dynamic singletons can be overridden, not the dynamic prototype " +
                    dynamicPrototypes.joinToString(", ") { it.getName() } + ")"
        }
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

        // Check for circular dependencies among non-lazy injections
        val cycles = dependencyLookUp.detectCircularDependencies()
        if (cycles.isNotEmpty()) {
            throw KontainerInconsistent(
                "Kontainer has circular dependencies!\n\n" +
                        cycles.joinToString("\n") { "  - $it" } + "\n\n" +
                        "Hint: Use Lazy<T> injection to break the cycle."
            )
        }

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

                "${serviceIdx + 1}. Service '$name'\n" +
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
