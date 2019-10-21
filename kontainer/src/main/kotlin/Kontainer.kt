package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * The container
 */
class Kontainer internal constructor(
    internal val blueprint: KontainerBlueprint,
    internal val providers: Map<KClass<*>, ServiceProvider>
) {
    /**
     * The root context is used, when services are directly requested from the Kontainer
     */
    private val rootContext = InjectionContext(this, Kontainer::class, Kontainer::class)

    // getting services ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get all service classes that would satisfy the given [cls]
     */
    fun <T : Any> getCandidates(cls: KClass<T>): Set<KClass<*>> = blueprint.superTypeLookup.getAllCandidatesFor(cls)

    /**
     * Get a service for the given class
     */
    inline fun <reified T : Any> get() = get(T::class)

    /**
     * Get a service for the given class
     */
    fun <T : Any> get(cls: KClass<T>): T = get(cls, rootContext)

    /**
     * Get a service for the given [cls] or null if no service can be provided
     */
    fun <T : Any> getOrNull(cls: KClass<T>): T? = getOrNull(cls, rootContext)

    /**
     * Get a service for the given [cls], and when it is present run the [block] on it.
     *
     * When the service is not present null is returned.
     * Otherwise the result of the [block] is returned.
     */
    fun <T : Any, R> use(cls: KClass<T>, block: T.() -> R?): R? {

        val type = blueprint.superTypeLookup.getDistinctForOrNull(cls) ?: return null

        return get(type).block()
    }

    /**
     * Get all services that are a super type of the given class
     */
    fun <T : Any> getAll(cls: KClass<T>): List<T> = getAll(cls, rootContext)

    /**
     * Get all services that are a super type of the given class as a [Lookup]
     */
    fun <T : Any> getLookup(cls: KClass<T>): Lookup<T> = getLookup(cls, rootContext)

    /**
     * Get a provider for the given service class
     */
    inline fun <reified T : Any> getProvider(): ServiceProvider = getProvider(T::class)

    /**
     * Get a provider for the given service class
     */
    fun <T : Any> getProvider(cls: KClass<T>): ServiceProvider {

        val type = blueprint.superTypeLookup.getDistinctFor(cls)

        return providers.getValue(type)
    }

    // getting parameters //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if there is a config value with the given [id] that is of the given [type]
     */
    fun hasConfig(id: String, type: KClass<*>) = blueprint.config[id].let { it != null && it::class == type }

    /**
     * Get a config value by its [id]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getConfig(id: String): T = blueprint.config[id] as T

    // debug ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun dump(): String {

        val maxServiceNameLength = providers.map { (k, _) -> (k.qualifiedName ?: "").length }.max() ?: 0

        val maxTypeLength = ServiceProvider.Type.values().map { it.toString().length }.max() ?: 0

        val services = providers.map { (k, v) ->
            "${(k.qualifiedName ?: "n/a").padEnd(maxServiceNameLength)} | " +
                    "${v.type.toString().padEnd(maxTypeLength)} | " +
                    if (v.createdAt != null) v.createdAt.toString() else "not created"
        }

        val maxConfigLength = blueprint.config.map { (k, _) -> k.length }.max() ?: 0

        val configs = blueprint.config
            .map { (k, v) -> "${k.padEnd(maxConfigLength)} | $v (${v::class.qualifiedName})" }
            .joinToString("\n")

        return "Kontainer dump:\n\n" +
                services.joinToString("\n") + "\n\n" +
                "Config values\n\n" +
                configs
    }

    // internal helpers ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internally gets a [service] for the given [context]
     */
    internal fun <T : Any> get(service: KClass<T>, context: InjectionContext): T {
        @Suppress("UNCHECKED_CAST")
        return getProvider(service).provide(context) as T
    }

    /**
     * Internally gets a [service] or null for the given [context]
     */
    internal fun <T : Any> getOrNull(service: KClass<T>, context: InjectionContext): T? {

        val type = blueprint.superTypeLookup.getDistinctForOrNull(service) ?: return null

        return get(type, context)
    }

    /**
     * Internally gets all super type services of [cls] as a [List]
     */
    internal fun <T : Any> getAll(cls: KClass<T>, context: InjectionContext): List<T> {
        @Suppress("UNCHECKED_CAST")
        return blueprint.superTypeLookup.getAllCandidatesFor(cls).map { get(it, context) as T }
    }

    /**
     * Internally gets all super type services of [cls] as a [Lookup]
     */
    internal fun <T : Any> getLookup(cls: KClass<T>, context: InjectionContext): Lookup<T> {
        @Suppress("UNCHECKED_CAST")
        return blueprint.superTypeLookup.getLookupBlueprint(cls).with(context) as Lookup<T>
    }
}
