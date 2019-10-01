package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.reflect

class KontainerBuilder internal constructor(builder: KontainerBuilder.() -> Unit) {

    private val config = mutableMapOf<String, Any>()

    private val definitions = mutableMapOf<KClass<*>, ServiceDefinition>(
        Kontainer::class to ServiceDefinition(
            Kontainer::class,
            InjectionType.Dynamic,
            Producer(listOf()) { kontainer, _ -> kontainer }
        )
    )

    private val definitionLocations = mutableMapOf<KClass<*>, StackTraceElement>()

    init {
        builder(this)
    }

    internal fun buildBlueprint(): KontainerBlueprint =
        KontainerBlueprint(config.toMap(), definitions.toMap(), definitionLocations.toMap())

    internal fun buildModule(): KontainerModule =
        KontainerModule(config.toMap(), definitions.toMap(), definitionLocations.toMap())

    private fun add(def: ServiceDefinition) = apply {
        definitions[def.produces] = def

        // we also record the location from where a service was recorded for better error message
        definitionLocations[def.produces] = Throwable().stackTrace.first {
            it.className != KontainerBuilder::class.qualifiedName
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Config values
    /////

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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Modules
    /////

    /**
     * Imports a module
     */
    fun module(module: KontainerModule) = apply {
        config.putAll(module.config)
        definitions.putAll(module.definitions)
        definitionLocations.putAll(module.definitionLocations)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Singleton Services
    /////

    /**
     * Register an already existing [instance] as a service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any> instance(instance: SRV) = add(
        ServiceDefinition(
            instance::class,
            InjectionType.Singleton,
            Producer(listOf()) { _, _ -> instance }
        )
    )

    /**
     * Register an already existing [instance] as a service
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> instance(srv: KClass<SRV>, instance: IMPL) = add(
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(listOf()) { _, _ -> instance }
        )
    )

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    fun <SRV : Any> singleton(srv: KClass<SRV>) = apply {

        if (srv.java.isInterface || srv.isAbstract) {
            throw InvalidClassProvided("A singleton service cannot be an interface or abstract class")
        }

        add(
            ServiceDefinition(
                srv,
                InjectionType.Singleton,
                Producer(srv.primaryConstructor!!.parameters) { _, params ->
                    srv.primaryConstructor!!.call(*params)
                }
            )
        )
    }

    /**
     * Registers a singleton service
     *
     * The service can by injected by the type [SRV] and its base types
     */
    inline fun <reified SRV : Any> singleton() = singleton(SRV::class)

    /**
     * Create a singleton via a factory method with zero injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> singleton0(srv: KClass<SRV>, factory: () -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }
        )
    )

    /**
     * Create a singleton via a factory method with zero injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> singleton0(noinline factory: () -> IMPL) =
        singleton0(SRV::class, factory)

    /**
     * Create a singleton via a factory method with one injected parameter
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> singleton(srv: KClass<SRV>, factory: (P1) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, (p1) ->
                factory.invoke(p1 as P1)
            }
        )
    )

    /**
     * Create a singleton via a factory method with one injected parameter
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1> singleton(noinline factory: (P1) -> IMPL) =
        singleton(SRV::class, factory)

    /**
     * Create a singleton via a factory method with two injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> singleton(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                factory.invoke(p1 as P1, p2 as P2)
            }
        )
    )

    /**
     * Create a singleton via a factory method with two injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2> singleton(noinline factory: (P1, P2) -> IMPL) =
        singleton(SRV::class, factory)

    /**
     * Create a singleton via a factory method with three injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> singleton(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }
        )
    )

    /**
     * Create a singleton via a factory method with three injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3> singleton(noinline factory: (P1, P2, P3) -> IMPL) =
        singleton(SRV::class, factory)


    /**
     * Create a singleton via a factory method with four injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> singleton(srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }
        )
    )

    /**
     * Create a singleton via a factory method with four injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4> singleton(noinline factory: (P1, P2, P3, P4) -> IMPL) =
        singleton(SRV::class, factory)

    /**
     * Create a singleton via a factory method with five injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }
        )
    )

    /**
     * Create a singleton via a factory method with five injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> singleton(
        noinline factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = singleton(SRV::class, factory)

    /**
     * Create a singleton via a factory method with six injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }
        )
    )

    /**
     * Create a singleton via a factory method with six injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> singleton(
        noinline factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = singleton(SRV::class, factory)

    /**
     * Create a singleton via a factory method with six injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> singleton(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Singleton,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }
        )
    )

    /**
     * Create a singleton via a factory method with seven injected parameters
     *
     * The service can by injected by the type [SRV] and its base types
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> singleton(
        noinline factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = singleton(SRV::class, factory)


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Prototype Services
    /////

    /**
     * Registers a prototype service
     *
     * The service can be injected by the type [SRV] or its base types
     */
    fun <SRV : Any> prototype(srv: KClass<SRV>) = apply {

        if (srv.java.isInterface || srv.isAbstract) {
            throw InvalidClassProvided("A prototype service cannot be an interface or abstract class")
        }

        add(
            ServiceDefinition(
                srv,
                InjectionType.Prototype,
                Producer(srv.primaryConstructor!!.parameters) { _, params ->
                    srv.primaryConstructor!!.call(*params)
                }
            )
        )
    }

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
    fun <SRV : Any, IMPL : SRV> prototype(srv: KClass<SRV>, impl: KClass<IMPL>) = apply {

        if (impl.java.isInterface || impl.isAbstract) {
            throw InvalidClassProvided("A prototype service cannot be an interface or abstract class")
        }

        add(
            ServiceDefinition(
                srv,
                InjectionType.Prototype,
                Producer(impl.primaryConstructor!!.parameters) { _, params ->
                    impl.primaryConstructor!!.call(*params)
                }
            )
        )
    }

    /**
     * Registers a prototype via a factory method with zero injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> prototype0(srv: KClass<SRV>, factory: () -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }
        )
    )

    /**
     * Registers a prototype via a factory method with zero injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> prototype0(noinline factory: () -> IMPL) =
        prototype0(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with one injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> prototype(srv: KClass<SRV>, factory: (P1) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, (p1) ->
                factory.invoke(p1 as P1)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with one injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1> prototype(noinline factory: (P1) -> IMPL) =
        prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with two injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> prototype(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                factory.invoke(p1 as P1, p2 as P2)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with two injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2> prototype(noinline factory: (P1, P2) -> IMPL) =
        prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with three injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> prototype(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with three injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3> prototype(noinline factory: (P1, P2, P3) -> IMPL) =
        prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with four injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> prototype(srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with four injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4> prototype(noinline factory: (P1, P2, P3, P4) -> IMPL) =
        prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with five injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with five injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> prototype(
        noinline factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with six injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with six injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> prototype(
        noinline factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = prototype(SRV::class, factory)

    /**
     * Registers a prototype via a factory method with seven injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> prototype(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Prototype,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }
        )
    )

    /**
     * Registers a prototype via a factory method with seven injected parameter
     *
     * The service can be injected by the type [SRV] or its base types.
     * The actual implementation will have the type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> prototype(
        noinline factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = prototype(SRV::class, factory)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dynamic Services
    /////

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     */
    fun <SRV : Any> dynamic(srv: KClass<SRV>) = add(
        ServiceDefinition(srv, InjectionType.Dynamic)
    )

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     */
    inline fun <reified SRV : Any> dynamic() = dynamic(SRV::class)

    /**
     * Registers a dynamic service
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> dynamic(srv: KClass<SRV>, impl: KClass<IMPL>) = apply {

        if (impl.java.isInterface || impl.isAbstract) {
            throw InvalidClassProvided("A dynamic service cannot be an interface or abstract class")
        }

        add(
            ServiceDefinition(
                srv,
                InjectionType.Dynamic,
                Producer(impl.primaryConstructor!!.parameters) { _, params ->
                    impl.primaryConstructor!!.call(*params)
                }
            )
        )
    }

    /**
     * Registers a dynamic service via a factory method with zero injected parameter
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV> dynamic0(srv: KClass<SRV>, factory: () -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with zero injected parameter
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV> dynamic0(noinline factory: () -> IMPL) =
        dynamic0(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with one injected parameter
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1> dynamic(srv: KClass<SRV>, factory: (P1) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, (p1) ->
                factory.invoke(p1 as P1)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with one injected parameter
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1> dynamic(noinline factory: (P1) -> IMPL) =
        dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with two injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2> dynamic(srv: KClass<SRV>, factory: (P1, P2) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                factory.invoke(p1 as P1, p2 as P2)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with two injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2> dynamic(noinline factory: (P1, P2) -> IMPL) =
        dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with three injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3> dynamic(srv: KClass<SRV>, factory: (P1, P2, P3) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with three injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3> dynamic(noinline factory: (P1, P2, P3) -> IMPL) =
        dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with four injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4> dynamic(srv: KClass<SRV>, factory: (P1, P2, P3, P4) -> IMPL) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with four injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4> dynamic(noinline factory: (P1, P2, P3, P4) -> IMPL) =
        dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with five injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with five injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5> dynamic(
        noinline factory: (P1, P2, P3, P4, P5) -> IMPL
    ) = dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with six injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with six injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6> dynamic(
        noinline factory: (P1, P2, P3, P4, P5, P6) -> IMPL
    ) = dynamic(SRV::class, factory)

    /**
     * Registers a dynamic service via a factory method with seven injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    fun <SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> dynamic(
        srv: KClass<SRV>, factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = add(
        @Suppress("UNCHECKED_CAST")
        ServiceDefinition(
            srv,
            InjectionType.Dynamic,
            Producer(factory.reflect()!!.parameters) { _, p ->
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }
        )
    )

    /**
     * Registers a dynamic service via a factory method with seven injected parameters
     *
     * The service can be injected by the type [SRV] and its base types.
     * The actual implementation is registered with type [IMPL]
     */
    inline fun <reified SRV : Any, IMPL : SRV, P1, P2, P3, P4, P5, P6, P7> dynamic(
        noinline factory: (P1, P2, P3, P4, P5, P6, P7) -> IMPL
    ) = dynamic(SRV::class, factory)
}
