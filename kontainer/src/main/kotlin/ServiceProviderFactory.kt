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
                val baseType = blueprint.dynamicsBaseTypeLookUp.getDistinctFor(cls)

                baseType to ServiceProvider.Provider.forInstance(
                    type = ServiceProvider.Type.DynamicOverride,
                    definition = ServiceDefinition(
                        produces = cls,
                        injectionType = InjectionType.Dynamic,
                        producer = ServiceProducer.forFactory(provider),
                        overwrites = blueprint.definitions[baseType]
                    ),
                    instance = provider(),
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

    fun getAllProviders(): Map<KClass<*>, ServiceProvider> = synchronized(lock) {

        providerProviders.mapValues { (cls, provider) ->
            providers.getOrPut(cls) {
                provider.provide()
            }
        }
    }
}
