package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * The container
 */
@Suppress("Detekt.TooManyFunctions")
class Kontainer internal constructor(
    private val factory: ServiceProviderFactory,
) : KontainerAware {
    /**
     * Tools for debugging the kontainer and more
     */
    val tools: KontainerTools = KontainerTools(this)

    /**
     * The blueprint for this kontainer
     */
    val blueprint: KontainerBlueprint = factory.blueprint

    /**
     * Implementing [KontainerAware]
     */
    override val kontainer: Kontainer = this

    /**
     * The root context is used, when services are directly requested from the Kontainer
     */
    private val rootContext: InjectionContext = InjectionContext.kontainerRoot

    /**
     * Get the underlying [ServiceProviderFactory].
     */
    fun getServiceProviderFactory(): ServiceProviderFactory = factory

    // Cloning the kontainer ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new fresh instance of this [Kontainer].
     *
     * This will reset all dynamic services, just like creating a new kontainer from a [KontainerBlueprint].
     */
    fun clone(dynamics: DynamicOverrides.Builder.() -> Unit = {}): Kontainer {
        return factory.newKontainer(
            DynamicOverrides.Builder().apply(dynamics).build()
        )
    }

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
     * Otherwise, the result of the [block] is returned.
     */
    inline fun <T : Any, R> use(cls: KClass<T>, block: T.() -> R?): R? {

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
    fun <T : Any> getLookup(cls: KClass<T>): LazyServiceLookup<T> = getLookup(cls, rootContext)

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

    // internal helpers ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internally gets a [service] for the given [context]
     */
    internal fun <T : Any> get(service: KClass<T>, context: InjectionContext): T {
        @Suppress("UNCHECKED_CAST")
        return getProvider(service).provide(this, context) as T
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
    internal fun <T : Any> getLookup(cls: KClass<T>, context: InjectionContext): LazyServiceLookup<T> {
        @Suppress("UNCHECKED_CAST")
        return blueprint.superTypeLookup.getLookupBlueprint(cls).with(this, context) as LazyServiceLookup<T>
    }
}
