package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Creates and caches [ServiceProvider] instances for a [Kontainer].
 *
 * Manages thread-safe access to providers and applies dynamic overrides.
 */
class ServiceProviderFactory(
    val blueprint: KontainerBlueprint,
    val dynamics: DynamicOverrides,
    providerProviders: Map<KClass<*>, ServiceProvider.Provider>,
) {
    private val lock: Any = Any()

    private val providerProviders: Map<KClass<*>, ServiceProvider.Provider> = providerProviders
        .plus(
            dynamics.overrides.map { (cls, create) ->
                val baseType = blueprint.dynamicsBaseTypeLookUp.getDistinctFor(cls)

                // The override runs at the first request, so until then its definition names the class given to with
                baseType to ServiceProvider.Provider.forOverride(
                    definition = ServiceDefinition(
                        serviceCls = cls,
                        type = ServiceType.Singleton,
                        kind = ServiceKind.Dynamic,
                        producer = ServiceProducer.forPendingOverride(cls),
                        overwrites = blueprint.definitions[baseType]
                    ),
                    create = create,
                )
            }
        )

    private val providers: MutableMap<KClass<*>, ServiceProvider> = mutableMapOf()

    /** Creates a new [Kontainer], merging in additional [overrides]; checked like [KontainerBlueprint.create] */
    fun newKontainer(overrides: DynamicOverrides): Kontainer {
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
