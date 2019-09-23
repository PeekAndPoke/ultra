package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The container
 */
data class Kontainer(
    val superTypeLookup: TypeLookup.ForSuperTypes,
    val config: Map<String, Any>,
    val providers: Map<KClass<*>, ServiceProvider>
) {

    // getting services ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get all service classes that would satisfy the given [cls]
     */
    fun <T : Any> getCandidates(cls: KClass<T>): Set<KClass<*>> = superTypeLookup.getAllCandidatesFor(cls)

    /**
     * Get a service for the given class
     */
    inline fun <reified T : Any> get() = get(T::class)

    /**
     * Get a service for the given class
     */
    fun <T : Any> get(cls: KClass<T>): T {
        @Suppress("UNCHECKED_CAST") return getProvider(cls).provide(this) as T
    }

    /**
     * Get a provider for the given service class
     */
    inline fun <reified T : Any> getProvider(): ServiceProvider = getProvider(T::class)

    /**
     * Get a provider for the given service class
     */
    fun <T : Any> getProvider(cls: KClass<T>): ServiceProvider {
        // We can safely assume that there is a exactly one base type around, as the container validation
        // must have found missing or ambiguous services already.
        val baseClass = superTypeLookup.getDistinctFor(cls)

        return providers.getValue(baseClass)
    }

    // getting parameters //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if there is a config value with the given [id] that is of the given [type]
     */
    fun hasConfig(id: String, type: KClass<*>) = config[id].let { it != null && it::class == type }

    /**
     * Get a config value by its [id]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getConfig(id: String) = config[id] as T
}
