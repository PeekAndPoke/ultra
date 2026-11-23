package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Creates and caches [ServiceProvider] instances for a [Kontainer].
 *
 * Manages thread-safe access to providers and applies dynamic overrides.
 */
class ServiceProviderFactory internal constructor(
    val blueprint: KontainerBlueprint,
    internal val dynamics: DynamicOverrides,
    providerProviders: Map<KClass<*>, ServiceProvider.Provider>,
) {
    private val lock: Any = Any()

    private val providerProviders: Map<KClass<*>, ServiceProvider.Provider> = providerProviders
        .plus(
            dynamics.overrides.map { (cls, provider) ->
                val baseType = blueprint.overriddenServiceOf(cls)

                // The override runs here, at create, once per kontainer; when it throws, no kontainer results
                val instance = provider()

                baseType to ServiceProvider.Provider.forOverride(
                    definition = ServiceDefinition(
                        serviceCls = cls,
                        type = ServiceType.Singleton,
                        kind = ServiceKind.Dynamic,
                        producer = ServiceProducer.forInstance(instance),
                        overwrites = blueprint.definitions[baseType]
                    ),
                    instance = instance,
                )
            }
        )

    private val providers: MutableMap<KClass<*>, ServiceProvider> = mutableMapOf()

    /** Creates a new [Kontainer] for [Kontainer.clone], merging in [overrides]; checked like [KontainerBlueprint.create] */
    internal fun newKontainer(overrides: DynamicOverrides): Kontainer {
        return blueprint.createChecked(
            dynamics.merge(overrides)
        )
    }

    /**
     * Get the [ServiceProducer] for the given [cls].
     */
    fun getProvider(cls: KClass<*>): ServiceProvider = synchronized(lock) {
        providers.getOrPut(cls) {
            providerProviders.getValue(cls).provide()
        }
    }

    /**
     * Checks if the [ServiceProvider] for the given [cls] is created.
     */
    fun isProviderCreated(cls: KClass<*>): Boolean = synchronized(lock) {
        providers.contains(cls)
    }

    /** Gets all providers, creating any that haven't been instantiated yet */
    fun getAllProviders(): Map<KClass<*>, ServiceProvider> = synchronized(lock) {

        providerProviders.mapValues { (cls, provider) ->
            providers.getOrPut(cls) {
                provider.provide()
            }
        }
    }
}
