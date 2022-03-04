package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.maxLineLength
import kotlin.reflect.KClass

/**
 * The container
 */
class Kontainer internal constructor(
    private val factory: ServiceProviderFactory,
) {
    /**
     * The blueprint for this kontainer
     */
    val blueprint: KontainerBlueprint = factory.blueprint

    /**
     * The root context is used, when services are directly requested from the Kontainer
     */
    private val rootContext: InjectionContext = InjectionContext(this, Kontainer::class, Kontainer::class)

    // Cloning the kontainer ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new fresh instance of this [Kontainer].
     *
     * This will reset all dynamic services, just like creating a new kontainer from a [KontainerBlueprint].
     */
    fun clone(): Kontainer = factory.newKontainer()

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

        return factory.getProvider(type)
    }

    // getting parameters //////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if there is a config value with the given [id] that is of the given [type]
     */
    fun hasConfig(id: String, type: KClass<*>): Boolean {
        return blueprint.configValues[id].let { it != null && it::class == type }
    }

    /**
     * Get a config value by its [id]
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getConfig(id: String): T = blueprint.configValues[id] as T

    // debug ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the underlying [ServiceProviderFactory].
     */
    fun getFactory(): ServiceProviderFactory = factory

    fun debugInfo(): KontainerDebugInfo {

        val config = blueprint.configValues
            .map { (k, v) -> k to "$v (${v::class.qualifiedName})" }
            .toMap()

        val services = factory.getAllProviders().map { provider ->
            KontainerDebugInfo.ServiceDebugInfo(
                id = provider.key.qualifiedName ?: "n/a",
                type = provider.value.type,
                instances = provider.value.instances.map { instance ->
                    KontainerDebugInfo.InstanceDebugInfo(
                        createdAt = instance.createdAt,
                        cls = instance.instance::class.qualifiedName ?: "n/a"
                    )
                }
            )
        }

        return KontainerDebugInfo(config = config, services = services)
    }

    fun dump(): String {

        val rows = mutableListOf(
            Triple("Service ID", "Type", "Instances")
        )

        rows.addAll(
            factory.getAllProviders().map { (k, v) ->
                Triple(
                    k.qualifiedName ?: "n/a",
                    v.type.toString(),
                    v.instances.joinToString("\n") { "${it.createdAt}: ${it.instance::class.qualifiedName}" }
                )
            }
        )

        val lens = Triple(
            rows.maxOfOrNull { it.first.maxLineLength() } ?: 0,
            rows.maxOfOrNull { it.second.maxLineLength() } ?: 0,
            rows.maxOfOrNull { it.third.maxLineLength() } ?: 0
        )

        val services = rows.map {
            "${it.first.padEnd(lens.first)} | ${it.second.padEnd(lens.second)} | ${it.third.padEnd(lens.third)}"
        }

        val maxConfigLength = blueprint.configValues.map { (k, _) -> k.length }.maxOrNull() ?: 0

        val configs = blueprint.configValues
            .map { (k, v) -> "${k.padEnd(maxConfigLength)} | $v (${v::class.qualifiedName})" }
            .joinToString("\n")

        return "Kontainer dump:" +
                "\n\n" +
                services.joinToString("\n") +
                "\n\n" +
                "Config values:" +
                "\n\n" +
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
