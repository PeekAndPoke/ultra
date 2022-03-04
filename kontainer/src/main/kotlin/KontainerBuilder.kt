package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

class KontainerBuilder internal constructor(builder: KontainerBuilder.() -> Unit) {

    private val configValues = mutableMapOf<String, Any>()

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
    internal fun build(
        config: KontainerBlueprint.Config = KontainerBlueprint.Config.default
    ): KontainerBlueprint =
        KontainerBlueprint(
            config = config,
            configValues = configValues.toMap(),
            definitions = definitions.toMap(),
            definitionLocations = definitionLocations.toMap()
        )

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
    fun config(id: String, value: Int) = apply { configValues[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Long) = apply { configValues[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Float) = apply { configValues[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Double) = apply { configValues[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: String) = apply { configValues[id] = value }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Boolean) = apply { configValues[id] = value }

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
    fun <SRV : Any, IMPL : SRV> singleton(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addSingleton(srv, ServiceProducer.forClass(impl))

    /**
     * Registers a singleton service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> singleton(srv: KClass<SRV>, factory: FAC) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> singleton0(srv: KClass<SRV>, factory: () -> IMPL) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> singleton(srv: KClass<SRV>, factory: (P1) -> IMPL) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> singleton(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> singleton(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL
    ) =
        addSingleton(srv, ServiceProducer.forFactory(factory))

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
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addPrototype(srv, ServiceProducer.forClass(impl))

    /**
     * Registers a prototype service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> prototype(srv: KClass<SRV>, factory: FAC) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a Prototype via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype0(srv: KClass<SRV>, factory: () -> IMPL) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> prototype(srv: KClass<SRV>, factory: (P1) -> IMPL) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> prototype(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> prototype(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL
    ) =
        addPrototype(srv, ServiceProducer.forFactory(factory))

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
     * Registers a dynamic service [SRV] with a default implementation [IMPL]
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual default implementation is registered with type [IMPL].
     */
    fun <SRV : Any, IMPL : SRV> dynamic(srv: KClass<SRV>, impl: KClass<IMPL>) =
        addDynamic(srv, ServiceProducer.forClass(impl))

    /**
     * Registers a dynamic service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> dynamic(srv: KClass<SRV>, factory: FAC) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic singleton via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> dynamic0(srv: KClass<SRV>, factory: () -> IMPL) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> dynamic(srv: KClass<SRV>, factory: (P1) -> IMPL) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> dynamic(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> dynamic(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV,
            P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL
    ) =
        addDynamic(srv, ServiceProducer.forFactory(factory))
}
