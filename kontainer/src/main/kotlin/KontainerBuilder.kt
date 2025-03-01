package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.full.createType

@Suppress("Detekt:TooManyFunctions")
class KontainerBuilder internal constructor(builder: KontainerBuilder.() -> Unit) {

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

    inner class ServiceBuilder internal constructor(
        private val fn: (cls: KClass<out Any>, producer: ServiceProducer<out Any>) -> Unit,
    ) {
        @PublishedApi
        internal fun add(cls: KClass<out Any>, producer: ServiceProducer<out Any>) {
            fn(cls, producer)
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        operator fun <SRV : Any> invoke(
            srv: KClass<SRV>,
        ) {
            add(srv, ServiceProducer.forClass(srv))
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        operator fun <SRV : Any, IMPL : SRV> invoke(
            srv: KClass<SRV>,
            impl: KClass<IMPL>,
        ) {
            return fn(srv, ServiceProducer.forClass(impl))
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        @JvmName("invoke_0_params")
        inline fun <SRV : Any, reified IMPL : SRV> factory(
            srv: KClass<SRV>,
            noinline factory: () -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(),
                    creates = IMPL::class,
                    factory = { factory() },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <SRV : Any, reified IMPL : SRV, reified P1> invoke(
            srv: KClass<SRV>,
            noinline factory: (P1) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1)
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <SRV : Any, reified IMPL : SRV, reified P1, reified P2> invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <SRV : Any, reified IMPL : SRV, reified P1, reified P2, reified P3> invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5, reified P6,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5, P6) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                        P6::class.createType(nullable = null is P6),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5,
                            p[5] as P6
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                        P6::class.createType(nullable = null is P6),
                        P7::class.createType(nullable = null is P7),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5,
                            p[5] as P6,
                            p[6] as P7
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                        P6::class.createType(nullable = null is P6),
                        P7::class.createType(nullable = null is P7),
                        P8::class.createType(nullable = null is P8),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5,
                            p[5] as P6,
                            p[6] as P7,
                            p[7] as P8,
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8,
                reified P9,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                        P6::class.createType(nullable = null is P6),
                        P7::class.createType(nullable = null is P7),
                        P8::class.createType(nullable = null is P8),
                        P9::class.createType(nullable = null is P9),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5,
                            p[5] as P6,
                            p[6] as P7,
                            p[7] as P8,
                            p[8] as P9,
                        )
                    },
                )
            )
        }

        /**
         * Registers a service
         *
         * The service can be injected by the type [SRV] and its base types
         */
        inline operator fun <
                SRV : Any, reified IMPL : SRV,
                reified P1, reified P2, reified P3, reified P4, reified P5, reified P6, reified P7, reified P8,
                reified P9, reified P10,
                > invoke(
            srv: KClass<SRV>,
            noinline factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> IMPL,
        ) {
            add(
                cls = srv,
                producer = ServiceProducer.forFactory(
                    params = listOf(
                        P1::class.createType(nullable = null is P1),
                        P2::class.createType(nullable = null is P2),
                        P3::class.createType(nullable = null is P3),
                        P4::class.createType(nullable = null is P4),
                        P5::class.createType(nullable = null is P5),
                        P6::class.createType(nullable = null is P6),
                        P7::class.createType(nullable = null is P7),
                        P8::class.createType(nullable = null is P8),
                        P9::class.createType(nullable = null is P9),
                        P10::class.createType(nullable = null is P10),
                    ),
                    creates = IMPL::class,
                    factory = { p ->
                        factory(
                            p[0] as P1,
                            p[1] as P2,
                            p[2] as P3,
                            p[3] as P4,
                            p[4] as P5,
                            p[5] as P6,
                            p[6] as P7,
                            p[7] as P8,
                            p[8] as P9,
                            p[9] as P10,
                        )
                    },
                )
            )
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singleton Services
    // //

    val singleton = ServiceBuilder { cls, producer ->
        @Suppress("UNCHECKED_CAST")
        addSingleton(cls as KClass<Any>, producer as ServiceProducer<Any>)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Prototype Services
    // //

    val prototype = ServiceBuilder { cls, producer ->
        @Suppress("UNCHECKED_CAST")
        addPrototype(cls as KClass<Any>, producer as ServiceProducer<Any>)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dynamic Services
    // //

    val dynamic = ServiceBuilder { cls, producer ->
        @Suppress("UNCHECKED_CAST")
        addDynamic(cls as KClass<Any>, producer as ServiceProducer<Any>)
    }

    init {
        builder(this)

        //
        // The kontainer is always present (dynamic as it changes with every new Kontainer instance)
        //
        addDynamic(Kontainer::class, ServiceProducer.forKontainer())

        //
        // The InjectionContext is always present and dynamic
        //
        // NOTICE: We provide a dummy ServiceProducer below.
        // The InjectionContext is injected via [ParameterProvider.ForInjectionContext]
        //
        addDynamic(InjectionContext::class, ServiceProducer.forInstance(InjectionContext.kontainerRoot))

        //
        // The blueprint is always present (singleton as it always stays the same)
        //
        addSingleton(KontainerBlueprint::class, ServiceProducer.forKontainerBlueprint())
    }

    /**
     * Builds a [KontainerBlueprint] from the current configuration
     */
    internal fun build(
        config: KontainerBlueprint.Config = KontainerBlueprint.Config.default,
    ): KontainerBlueprint {
        return KontainerBlueprint(
            config = config,
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
    private fun <SRV : Any, IMPL : SRV> add(
        serviceCls: KClass<SRV>,
        type: InjectionType,
        producer: ServiceProducer<IMPL>,
    ): KontainerBuilder = apply {

        val service = ServiceDefinition(
            serviceCls = serviceCls,
            injectionType = type,
            producer = producer,
            overwrites = definitions[serviceCls]
        ).withTypeUpgrade()

        definitions[service.serviceCls] = service
    }

    /**
     * Adds a singleton service definition
     */
    private fun <SRV : Any, IMPL : SRV> addSingleton(
        serviceCls: KClass<SRV>,
        producer: ServiceProducer<out IMPL>,
    ): KontainerBuilder {
        return add(serviceCls, InjectionType.Singleton, producer)
    }

    /**
     * Adds a prototype service definition
     */
    private fun <SRV : Any, IMPL : SRV> addPrototype(
        srv: KClass<SRV>,
        producer: ServiceProducer<IMPL>,
    ): KontainerBuilder {
        return add(srv, InjectionType.Prototype, producer)
    }

    /**
     * Adds a dynamic service definition
     */
    private fun <SRV : Any, IMPL : SRV> addDynamic(
        srv: KClass<SRV>,
        producer: ServiceProducer<IMPL>,
    ): KontainerBuilder {
        return add(srv, InjectionType.Dynamic, producer)
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modules
    // //

    /** Imports a module
     */
    fun module(
        module: KontainerModule,
    ): KontainerBuilder = apply {module.apply(this) }

    /** Imports a module */
    operator fun KontainerModule.invoke() {
        module(this)
    }

    /** Imports a parameterized module */
    fun <P> module(module: ParameterizedKontainerModule<P>, p1: P): KontainerBuilder = apply {
        module.apply(this, p1)
    }

    /** Imports a parameterized module */
    fun <P1, P2> module(
        module: ParameterizedKontainerModule2<P1, P2>,
        p1: P1, p2: P2,
    ): KontainerBuilder = apply { module.apply(this, p1, p2) }

    /** Imports a parameterized module */
    fun <P1, P2, P3> module(
        module: ParameterizedKontainerModule3<P1, P2, P3>,
        p1: P1, p2: P2, p3: P3,
    ): KontainerBuilder = apply {
        module.apply(this, p1, p2, p3)
    }

    /** Imports a parameterized module */
    fun <P1, P2, P3, P4> module(
        module: ParameterizedKontainerModule4<P1, P2, P3, P4>,
        p1: P1, p2: P2, p3: P3, p4: P4,
    ): KontainerBuilder = apply {
        module.apply(this, p1, p2, p3, p4)
    }

    /** Imports a parameterized module */
    fun <P1, P2, P3, P4, P5> module(
        module: ParameterizedKontainerModule5<P1, P2, P3, P4, P5>,
        p1: P1, p2: P2, p3: P3, p4: P4, p5: P5,
    ): KontainerBuilder = apply {
        module.apply(this, p1, p2, p3, p4, p5)
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
        @Suppress("UNCHECKED_CAST")
        return addSingleton(
            serviceCls = instance::class as KClass<SRV>,
            ServiceProducer.forInstance(instance)
        )
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
}
