package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

@Suppress("Detekt:TooManyFunctions")
class KontainerBuilder internal constructor(builder: KontainerBuilder.() -> Unit) {

    private val configValues = mutableMapOf<String, Any>()

    private val definitions = mutableMapOf<KClass<*>, ServiceDefinition>()

    private val injectionTypeUpgrade = InjectionTypeUpgrade()

    class InjectionTypeUpgrade {

        fun adjust(def: ServiceDefinition): ServiceDefinition {
            return adjust(newDef = def, existingDef = def.overwrites)
        }

        fun adjust(newDef: ServiceDefinition, existingDef: ServiceDefinition?): ServiceDefinition {
            return when (existingDef) {
                // When there is no existing definition, we can use the new definition as is
                null -> newDef

                else -> when (existingDef.injectionType) {
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
    ): KontainerBlueprint {
        return KontainerBlueprint(
            config = config,
            configValues = configValues.toMap(),
            definitions = definitions.toMap(),
        )
    }

    /**
     * Performs injection type upgrade.
     *
     * See [injectionTypeUpgrade] and [InjectionTypeUpgrade].
     */
    private fun ServiceDefinition.withTypeUpgrade() = injectionTypeUpgrade.adjust(this)

    /**
     * Add a service with the given parameters
     */
    private fun <T : Any> add(
        produces: KClass<T>,
        type: InjectionType,
        producer: ServiceProducer,
    ): KontainerBuilder = apply {

        val service = ServiceDefinition(
            produces = produces,
            injectionType = type,
            producer = producer,
            overwrites = definitions[produces]
        ).withTypeUpgrade()

        definitions[service.produces] = service
    }

    /**
     * Adds a singleton service definition
     */
    private fun <T : Any> addSingleton(
        srv: KClass<T>,
        producer: ServiceProducer,
    ): KontainerBuilder {
        return add(srv, InjectionType.Singleton, producer)
    }

    /**
     * Adds a prototype service definition
     */
    private fun <T : Any> addPrototype(
        srv: KClass<T>,
        producer: ServiceProducer,
    ): KontainerBuilder {
        return add(srv, InjectionType.Prototype, producer)
    }

    /**
     * Adds a dynamic service definition
     */
    private fun <T : Any> addDynamic(
        srv: KClass<T>,
        producer: ServiceProducer,
    ): KontainerBuilder {
        return add(srv, InjectionType.Dynamic, producer)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Config values
    // //

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Int): KontainerBuilder = apply {
        configValues[id] = value
    }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Long): KontainerBuilder = apply {
        configValues[id] = value
    }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Float): KontainerBuilder = apply {
        configValues[id] = value
    }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Double): KontainerBuilder = apply {
        configValues[id] = value
    }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: String): KontainerBuilder = apply {
        configValues[id] = value
    }

    /**
     * Sets an injectable config value
     */
    fun config(id: String, value: Boolean): KontainerBuilder = apply {
        configValues[id] = value
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modules
    // //

    /**
     * Imports a module
     */
    fun module(
        module: KontainerModule
    ): KontainerBuilder = apply {
        module.apply(this)
    }

    /**
     * Imports a parameterized module
     */
    fun <P> module(
        module: ParameterizedKontainerModule<P>,
        param: P,
    ): KontainerBuilder = apply {
        module.apply(this, param)
    }

    /**
     * Imports a parameterized module
     */
    fun <P1, P2> module(
        module: ParameterizedKontainerModule2<P1, P2>,
        p1: P1,
        p2: P2,
    ): KontainerBuilder = apply {
        module.apply(this, p1, p2)
    }

    /**
     * Imports a parameterized module
     */
    fun <P1, P2, P3> module(
        module: ParameterizedKontainerModule3<P1, P2, P3>,
        p1: P1,
        p2: P2,
        p3: P3,
    ): KontainerBuilder = apply {
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
    fun <SRV : Any> instance(
        instance: SRV,
    ): KontainerBuilder {
        return addSingleton(instance::class, ServiceProducer.forInstance(instance))
    }

    /**
     * Register an already existing [instance] as a service
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> instance(
        srv: KClass<SRV>,
        instance: IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forInstance(instance))
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singleton Services
    // //

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any> singleton(
        srv: KClass<SRV>,
    ): KontainerBuilder {
        return singleton(srv, srv)
    }

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV> singleton(
        srv: KClass<SRV>,
        impl: KClass<IMPL>,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forClass(impl))
    }

    /**
     * Registers a singleton service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> singleton(
        srv: KClass<SRV>,
        factory: FAC,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> singleton0(
        srv: KClass<SRV>,
        factory: () -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> singleton(
        srv: KClass<SRV>,
        factory: (P1) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a singleton via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> singleton(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL,
    ): KontainerBuilder {
        return addSingleton(srv, ServiceProducer.forFactory(factory))
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Prototype Services
    // //

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types
     */
    fun <SRV : Any> prototype(
        srv: KClass<SRV>,
    ): KontainerBuilder {
        return prototype(srv, srv)
    }

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype(
        srv: KClass<SRV>,
        impl: KClass<IMPL>,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forClass(impl))
    }

    /**
     * Registers a prototype service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> prototype(
        srv: KClass<SRV>,
        factory: FAC,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a Prototype via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype0(
        srv: KClass<SRV>,
        factory: () -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> prototype(
        srv: KClass<SRV>,
        factory: (P1) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a prototype via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> prototype(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL,
    ): KontainerBuilder {
        return addPrototype(srv, ServiceProducer.forFactory(factory))
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dynamic Services
    // //

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     */
    fun <SRV : Any> dynamic(
        srv: KClass<SRV>,
    ): KontainerBuilder {
        return dynamic(srv, srv)
    }

    /**
     * Registers a dynamic service [SRV] with a default implementation [IMPL]
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual default implementation is registered with type [IMPL].
     */
    fun <SRV : Any, IMPL : SRV> dynamic(
        srv: KClass<SRV>,
        impl: KClass<IMPL>,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forClass(impl))
    }

    /**
     * Registers a dynamic service with variable number of parameters
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any, IMPL : SRV, FAC : Function<IMPL>> dynamic(
        srv: KClass<SRV>,
        factory: FAC,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic singleton via a factory method with 0 injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> dynamic0(
        srv: KClass<SRV>,
        factory: () -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> dynamic(
        srv: KClass<SRV>,
        factory: (P1) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }

    /**
     * Create a dynamic service via a factory
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> dynamic(
        srv: KClass<SRV>,
        factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> IMPL,
    ): KontainerBuilder {
        return addDynamic(srv, ServiceProducer.forFactory(factory))
    }
}
