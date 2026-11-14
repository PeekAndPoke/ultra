package io.peekandpoke.ultra.kontainer

import java.time.Instant

/**
 * Service providers create instances of services
 */
interface ServiceProvider {

    /**
     * Provides a [ServiceProvider]
     */
    interface Provider {

        companion object {
            /** An instance handed in at [KontainerBlueprint.create], replacing a dynamic singleton */
            fun forOverride(definition: ServiceDefinition, create: () -> Any): Provider {
                return OverrideProvider(definition = definition, create = create)
            }

            /** A static singleton: one provider, so one instance, for all kontainers of the blueprint */
            fun forGlobalSingleton(definition: ServiceDefinition): Provider {
                return ForGlobalSingleton(definition = definition)
            }

            /** A dynamic or semi dynamic singleton: one provider, so one instance, per kontainer */
            fun forDynamicSingleton(kind: ServiceKind, definition: ServiceDefinition): Provider {
                require(kind != ServiceKind.Static) {
                    "A per-kontainer singleton is Dynamic or SemiDynamic, not Static"
                }

                return ForDynamicSingleton(kind = kind, definition = definition)
            }
        }

        private class OverrideProvider(definition: ServiceDefinition, create: () -> Any) : Provider {

            private val provider = ForOverride(pending = definition, create = create)

            override fun provide(): ForOverride {
                return provider
            }
        }

        private class ForGlobalSingleton(definition: ServiceDefinition) : Provider {

            private val provider = ForSingleton(kind = ServiceKind.Static, definition = definition)

            override fun provide(): ForSingleton {
                return provider
            }
        }

        private class ForDynamicSingleton(
            private val kind: ServiceKind,
            private val definition: ServiceDefinition,
        ) : Provider {

            override fun provide(): ForSingleton {
                return ForSingleton(kind = kind, definition = definition)
            }
        }

        fun provide(): ServiceProvider
    }

    /**
     * Tracks created service instances
     *
     * [instance] is the service instance that was created.
     * [createdAt] is the timestamp, when the instantiation occurred.
     */
    data class CreatedInstance(
        val instance: Any,
        val createdAt: Instant,
    )

    /**
     * What the service is: singleton or prototype.
     */
    val type: ServiceType

    /**
     * What the service is bound to, as derived by the blueprint.
     */
    val kind: ServiceKind

    /**
     * True when the instance was handed in at [KontainerBlueprint.create], replacing a dynamic singleton.
     */
    val overridden: Boolean

    /**
     * A list of all instances created by the provider.
     */
    val instances: List<CreatedInstance>

    /**
     * The definition of the service.
     */
    val definition: ServiceDefinition

    /**
     * Provides the service instance
     */
    fun provide(kontainer: Kontainer, context: InjectionContext): Any

    /**
     * Validates that a service can be provided.
     *
     * When all is well an empty list is returned.
     * Otherwise, a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    /**
     * Provides an instance handed in at [KontainerBlueprint.create], replacing a dynamic singleton.
     *
     * The override runs at the first request, again at the next one when it threw, until it has returned a value;
     * then never again, and only its value is kept, not the lambda.
     */
    class ForOverride(
        private val pending: ServiceDefinition,
        create: () -> Any,
    ) : ServiceProvider {

        /** The override, until it has returned a value */
        @Volatile
        private var create: (() -> Any)? = create

        @Volatile
        private var instance: Any? = null

        /** The definition after the override ran, naming the class it created */
        @Volatile
        private var created: ServiceDefinition? = null

        /** Always a singleton */
        override val type: ServiceType = ServiceType.Singleton

        /** Always dynamic: only dynamic singletons can be overridden */
        override val kind: ServiceKind = ServiceKind.Dynamic

        /** Always overridden */
        override val overridden: Boolean = true

        /**
         * Until the override has returned a value: names the class given to `with`. After: the class of the instance.
         */
        override val definition: ServiceDefinition get() = created ?: pending

        /**
         * Empty until the override has returned a value, then the one instance
         */
        override val instances = mutableListOf<CreatedInstance>()

        /**
         * Runs the override at the first request and keeps its value
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any {
            return instance ?: synchronized(this) {
                instance ?: create!!().also { value ->
                    created = ServiceDefinition(
                        serviceCls = pending.serviceCls,
                        type = pending.type,
                        kind = pending.kind,
                        producer = ServiceProducer.forInstance(value),
                        overwrites = pending.overwrites,
                        codeLocation = pending.codeLocation,
                    )
                    instances.add(CreatedInstance(value, Instant.now()))
                    instance = value
                    create = null
                }
            }
        }

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer) = listOf<String>()
    }

    /**
     * Provides a global singleton service
     */
    data class ForSingleton(
        override val kind: ServiceKind,
        override val definition: ServiceDefinition,
    ) : ServiceProvider {

        /** Always a singleton */
        override val type: ServiceType = ServiceType.Singleton

        /** Never handed in at create */
        override val overridden: Boolean = false

        /**
         * True when the service instance was created
         */
        override val instances = mutableListOf<CreatedInstance>()

        @Volatile
        private var instance: Any? = null

        /**
         * Get or create the instance of the service
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any {
            return instance ?: synchronized(this) {
                instance ?: create(kontainer, context).apply {
                    instance = this
                    instances.add(CreatedInstance(this, Instant.now()))
                }
            }
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.params.flatMap {
            it.validate(kontainer)
        }

        /**
         * Creates a new instance
         */
        private fun create(kontainer: Kontainer, context: InjectionContext): Any {

            val nextCtx = context.next(definition.serviceCls)

            return definition.producer.creator(
                kontainer,
                definition.producer.params.map {
                    it.provide(
                        kontainer = kontainer,
                        context = nextCtx
                    )
                }.toTypedArray()
            )
        }
    }

    /**
     * Provides a prototype service
     *
     * Each call to [provide] will create a new instance.
     */
    data class ForPrototype(
        override val kind: ServiceKind,
        override val definition: ServiceDefinition,
    ) : ServiceProvider {

        /** One provider per kontainer, whatever the [kind] */
        class Provider(
            private val kind: ServiceKind,
            private val definition: ServiceDefinition,
        ) : ServiceProvider.Provider {

            override fun provide(): ForPrototype {
                return ForPrototype(kind = kind, definition = definition)
            }
        }

        /** Always a prototype */
        override val type: ServiceType = ServiceType.Prototype

        /** Never handed in at create */
        override val overridden: Boolean = false

        /**
         * True when the service instance was created
         */
        override val instances = mutableListOf<CreatedInstance>()

        /**
         * Get or create the instance of the service
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any {
            return synchronized(this) {
                val instance = create(kontainer, context)

                instances.add(CreatedInstance(instance, Instant.now()))

                instance
            }
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.params.flatMap {
            it.validate(kontainer)
        }

        /**
         * Creates a new instance
         */
        private fun create(kontainer: Kontainer, context: InjectionContext): Any {

            val nextContext = context.next(definition.serviceCls)

            return definition.producer.creator(
                kontainer,
                definition.producer.params.map {
                    it.provide(
                        kontainer = kontainer,
                        context = nextContext,
                    )
                }.toTypedArray()
            )
        }
    }
}
