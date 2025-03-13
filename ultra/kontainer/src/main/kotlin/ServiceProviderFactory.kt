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

                val instance = provider()

                baseType to ServiceProvider.Provider.forInstance(
                    type = ServiceProvider.Type.DynamicOverride,
                    definition = ServiceDefinition(
                        serviceCls = cls,
                        injectionType = InjectionType.Dynamic,
                        producer = ServiceProducer.forInstance(instance),
                        overwrites = blueprint.definitions[baseType]
                    ),
                    instance = instance,
                )
            }
        )

    private val providers: MutableMap<KClass<*>, ServiceProvider> = mutableMapOf()

    // TODO: test me
    fun newKontainer(overrides: DynamicOverrides): Kontainer {
        return blueprint.instantiate(
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

    fun getAllProviders(): Map<KClass<*>, ServiceProvider> = synchronized(lock) {

        providerProviders.mapValues { (cls, provider) ->
            providers.getOrPut(cls) {
                provider.provide()
            }
        }
    }
}
