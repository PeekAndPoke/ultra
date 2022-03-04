package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

class ServiceProviderFactory(
    val blueprint: KontainerBlueprint,
    val dynamics: DynamicOverrides,
    providerProviders: Map<KClass<*>, ServiceProvider.Provider>,
) {
    private val lock: Any = Any()

    private val providerProviders: Map<KClass<*>, ServiceProvider.Provider> = providerProviders
        .plus(
            dynamics.overrides.map { (cls, provider) ->
                blueprint.dynamicsBaseTypeLookUp.getDistinctFor(cls) to ServiceProvider.Provider.forInstance(
                    ServiceProvider.Type.DynamicOverride,
                    provider()
                )
            }
        )

    private val providers: MutableMap<KClass<*>, ServiceProvider> = mutableMapOf()

    fun newKontainer() = blueprint.instantiate(dynamics)

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

    fun getAllProviders() = synchronized(lock) {

        providerProviders.mapValues { (cls, provider) ->
            providers.getOrPut(cls) {
                provider.provide()
            }
        }
    }
}
