package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

internal class ServiceProviderRegistry(
    val blueprint: KontainerBlueprint,
    private val dynamics: DynamicOverrides,
) {
    val map: Map<KClass<*>, ServiceProvider> = createMap()

    fun newKontainer() = blueprint.instantiate(dynamics)

    private fun createMap(): Map<KClass<*>, ServiceProvider> = with(blueprint) {

        val overwrittenDynamics = dynamics.overrides.map { (cls, provider) ->
            blueprint.dynamicsBaseTypeLookUp.getDistinctFor(cls) to ServiceProvider.ForInstance(
                ServiceProvider.Type.DynamicOverride,
                provider()
            )
        }

        val map = HashMap<KClass<*>, ServiceProvider>(
            singletons.size +
                    dynamicDefinitions.size +
                    prototypeDefinitions.size +
                    semiDynamicDefinitions.size +
                    overwrittenDynamics.size
        )

        // put all singletons
        map.putAll(singletons)

        // add providers for prototype services
        map.putAll(prototypeDefinitions.mapValues { (_, v) -> ServiceProvider.ForPrototype(v) })

        // create new providers for all semi dynamic services
        semiDynamicDefinitions
            .forEach { (k, v) -> map[k] = ServiceProvider.ForSingleton(ServiceProvider.Type.SemiDynamic, v) }

        // create new providers for all dynamic services
        dynamicDefinitions
            .forEach { (k, v) -> map[k] = ServiceProvider.ForSingleton(ServiceProvider.Type.Dynamic, v) }

        // add providers for all overwritten dynamic services
        map.putAll(overwrittenDynamics)

        map
    }
}
