package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * The container
 */
data class Kontainer internal constructor(
    val superTypeLookup: TypeLookup.ForSuperTypes,
    val config: Map<String, Any>,
    val providers: Map<KClass<*>, ServiceProvider>
) {

    // getting services ////////////////////////////////////////////////////////////////////////////////////////////////

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
     * Get all services that are a super type of the given class
     */
    fun <T : Any> getAll(cls: KClass<T>): List<T> {

        @Suppress("UNCHECKED_CAST")
        return superTypeLookup.getAllCandidatesFor(cls).map { get(it) as T }
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

    // getting parameters //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if there is a config value with the given [id] that is of the given [type]
     */
    fun hasConfig(id: String, type: KClass<*>) = config[id].let { it != null && it::class == type }

    /**
     * Get a config value by its [id]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getConfig(id: String): T = config[id] as T

    // debug ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun dump(): String {

        val services = providers.map { (k, v) ->
            "${(k.qualifiedName ?: "n/a").padEnd(50)} | " +
                    "${v.type.toString().padEnd(20)} | " +
                    if (v.isCreated) "created" else "NOT created"
        }

        val configs = config.map { (k, v) -> "${k.padEnd(20)} | $v (${v::class.qualifiedName})" }
            .joinToString("\n")

        return "Kontainer dump:\n\n" +
                services.joinToString("\n") + "\n\n" +
                "Config values\n\n" +
                configs
    }
}
