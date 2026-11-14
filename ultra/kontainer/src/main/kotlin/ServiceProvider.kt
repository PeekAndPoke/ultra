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
            fun forOverride(definition: ServiceDefinition, instance: Any): Provider {
                return ForOverride(definition = definition, instance = instance)
            }

            /** A static singleton: one provider, so one instance, for all kontainers of the blueprint */
            fun forGlobalSingleton(definition: ServiceDefinition): Provider {
                return ForGlobalSingleton(definition = definition)
            }

            /** A dynamic or semi dynamic singleton: one provider, so one instance, per kontainer */
            fun forDynamicSingleton(kind: ServiceKind, definition: ServiceDefinition): Provider {
                return ForDynamicSingleton(kind = kind, definition = definition)
            }
        }

        private class ForOverride(definition: ServiceDefinition, instance: Any) : Provider {

            private val provider = ForInstance(
                kind = ServiceKind.Dynamic,
                overridden = true,
                definition = definition,
                instance = instance,
            )

            override fun provide(): ForInstance {
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
     * Provides an already existing object as a service
     */
    data class ForInstance(
        override val kind: ServiceKind,
        override val overridden: Boolean,
        override val definition: ServiceDefinition,
        private val instance: Any,
    ) : ServiceProvider {

        /** Always a singleton */
        override val type: ServiceType = ServiceType.Singleton

        /**
         * The list of created instance always holds the [instance]
         */
        override val instances = listOf(
            CreatedInstance(instance, Instant.now())
        )

        /**
         * Simply returns the [instance]
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext) = instance

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
