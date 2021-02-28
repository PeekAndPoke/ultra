package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

class KontainerBuilder internal constructor(builder: KontainerBuilder.() -> Unit) {

    private val config = mutableMapOf<String, Any>()

    private val definitions = mutableMapOf<KClass<*>, ServiceDefinition>()

    private val definitionLocations = mutableMapOf<KClass<*>, StackTraceElement>()

    private val injectionTypeUpgrade = InjectionTypeUpgrade()

    class InjectionTypeUpgrade {

        fun adjust(newDef: ServiceDefinition, existingDef: ServiceDefinition?) = when (existingDef) {

            // When there is no existing definition, we can use the new definition as is
            null -> newDef

            else -> when (existingDef.type) {
                // When the existing definition is a prototype,
                InjectionType.Prototype ->
                    // Then we need to upgrade the new definition to a prototype as well
                    newDef.withType(InjectionType.Prototype)

                // When the existing definition is a dynamic
                InjectionType.Dynamic ->
                    // Then the type will stay dynamic
                    newDef.withType(InjectionType.Dynamic)

                // When the existing definition is a singleton
                InjectionType.Singleton ->
                    // Then we can use it as is
                    newDef
            }
        }
    }

    init {
        builder(this)

        // The container is always present (dynamic as it changes with every new Kontainer instance)
        addDynamic(Kontainer::class, ServiceProducer.forKontainer())

        // The blueprint is always present (singleton as it always stays the same)
        addSingleton(KontainerBlueprint::class, ServiceProducer.forKontainerBlueprint())
    }

    /**
     * Builds a [KontainerBlueprint] from the current configuration
     */
    internal fun build(): KontainerBlueprint =
        KontainerBlueprint(config.toMap(), definitions.toMap(), definitionLocations.toMap())

    /**
     * Adds a [ServiceDefinition] while also upgrading the injection type
     */
    private fun add(def: ServiceDefinition) = apply {

        // We need to  upgrade the type of the service definition, when necessary
        val upgrade = injectionTypeUpgrade.adjust(def, definitions[def.produces])

        // We use the upgraded definition
        definitions[def.produces] = upgrade

        // we also record the location from where a service was recorded for better error message
        definitionLocations[def.produces] = Throwable().stackTrace.first {
            it.className != KontainerBuilder::class.qualifiedName
        }
    }

    /**
     * Add a service with the given parameters
     */
    private fun <T : Any> add(srv: KClass<T>, type: InjectionType, producer: ServiceProducer) = add(
        ServiceDefinition(srv, type, producer)
    )

    /**
     * Adds a singleton service definition
     */
    private fun <T : Any> addSingleton(srv: KClass<T>, producer: ServiceProducer) =
        add(srv, InjectionType.Singleton, producer)

    /**
     * Adds a prototype service definition
     */
    private fun <T : Any> addPrototype(srv: KClass<T>, producer: ServiceProducer) =
        add(srv, InjectionType.Prototype, producer)

    /**
     * Adds a dynamic service definition
     */
    private fun <T : Any> addDynamic(srv: KClass<T>, producer: ServiceProducer) =
        add(srv, InjectionType.Dynamic, producer)

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Config values
    // //

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Int) = apply { config[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Long) = apply { config[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Float) = apply { config[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Double) = apply { config[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: String) = apply { config[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Boolean) = apply { config[id] = value }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modules
    // //

    /**
     * Imports a module
     */
    fun module(module: KontainerModule) = apply {
        module.apply(this)
    }

    /**
     * Imports a parameterized module
     */
    fun <P> module(module: ParameterizedKontainerModule<P>, param: P) = apply {
        module.apply(this, param)
    }

    /**
     * Imports a parameterized module
     */
    fun <P1, P2> module(module: ParameterizedKontainerModule2<P1, P2>, p1: P1, p2: P2) = apply {
        module.apply(this, p1, p2)
    }

    /**
     * Imports a parameterized module
     */
    fun <P1, P2, P3> module(module: ParameterizedKontainerModule3<P1, P2, P3>, p1: P1, p2: P2, p3: P3) = apply {
        module.apply(this, p1, p2, p3)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singleton Instances
    // //

    /**
     * Register an already existing [instance] as a service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any> instance(instance: SRV) =
        addSingleton(instance::class, ServiceProducer.forInstance(instance))

    /**
     * Register an already existing [instance] as a service
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> instance(srv: KClass<SRV>, instance: IMPL) =
        addSingleton(srv, ServiceProducer.forInstance(instance))

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singleton Services
    // //

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any> singleton(srv: KClass<SRV>) = singleton(srv, srv)

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    inline fun <reified SRV : Any> singleton() = singleton(SRV::class)

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV> singleton(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addSingleton(srv, ServiceProducer.forClass(impl))

    /**
     * Create a singleton via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> singleton(srv: KClass<SRV>, factory: Function<IMPL>) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> singleton(factory: Function<IMPL>) =
        singleton(SRV::class, factory)

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Prototype Services
    // //

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types
     */
    fun <SRV : Any> prototype(srv: KClass<SRV>) = prototype(srv, srv)

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types
     */
    inline fun <reified SRV : Any> prototype() = prototype(SRV::class)

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addPrototype(srv, ServiceProducer.forClass(impl))

    /**
     * Create a Prototype via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype(srv: KClass<SRV>, factory: Function<IMPL>) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a Prototype via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> prototype(factory: Function<IMPL>) =
        prototype(SRV::class, factory)

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dynamic Services
    // //

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     */
    fun <SRV : Any> dynamic(srv: KClass<SRV>) = dynamic(srv, srv)

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     */
    inline fun <reified SRV : Any> dynamic() = dynamic(SRV::class)

    /**
     * Registers a dynamic service [SRV] with a default implementation [IMPL]
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual default implementation is registered with type [IMPL].
     */
    fun <SRV : Any, IMPL : SRV> dynamic(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addDynamic(srv, ServiceProducer.forClass(impl))

    /**
     * Create a dynamic singleton via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> dynamic(srv: KClass<SRV>, factory: Function<IMPL>) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory method with up to 10 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> dynamic(factory: Function<IMPL>) =
        dynamic(SRV::class, factory)
}
