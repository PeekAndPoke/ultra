package de.peekandpoke.ultra.kontainer

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
            fun forInstance(type: Type, definition: ServiceDefinition, instance: Any): Provider {
                return ForInstance(type = type, definition = definition, instance = instance)
            }

            fun forGlobalSingleton(type: Type, definition: ServiceDefinition): Provider {
                return ForGlobalSingleton(type = type, definition = definition)
            }

            fun forDynamicSingleton(type: Type, definition: ServiceDefinition): Provider {
                return ForDynamicSingleton(type = type, definition = definition)
            }
        }

        private class ForInstance(type: Type, definition: ServiceDefinition, instance: Any) : Provider {

            private val provider =
                ServiceProvider.ForInstance(type = type, definition = definition, instance = instance)

            override fun provide(): ServiceProvider.ForInstance {
                return provider
            }
        }

        private class ForGlobalSingleton(type: Type, definition: ServiceDefinition) : Provider {

            private val provider = ForSingleton(type = type, definition = definition)

            override fun provide(): ForSingleton {
                return provider
            }
        }

        private class ForDynamicSingleton(
            private val type: Type,
            private val definition: ServiceDefinition
        ) : Provider {

            override fun provide(): ForSingleton {
                return ForSingleton(type = type, definition = definition)
            }
        }

        fun provide(): ServiceProvider
    }

    /**
     * Ways in which services are provided.
     *
     * This is mostly used for debugging and testing.
     */
    enum class Type {
        /** Pure singleton services are shared across multiple Kontainer instances */
        Singleton,

        /** Prototype service are instantiate for each injection */
        Prototype,

        /** Semi Dynamic singletons are not defined as dynamic but inject dynamic services */
        SemiDynamic,

        /** Dynamic singletons are explicitly defined as dynamic and live only within a single Kontainer instance */
        Dynamic,

        /** Like Dynamic singletons, but these are overriding dynamics while creating a Kontainer from a Blueprint */
        DynamicOverride,
    }

    /**
     * Tracks created service instances
     *
     * [instance] is the service instance that was created.
     * [createdAt] is the timestamp, when the instantiation occurred.
     */
    data class CreatedInstance(
        val instance: Any,
        val createdAt: Instant
    )

    /**
     * The type of the service that is created.
     */
    val type: Type

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
    fun provide(context: InjectionContext): Any

    /**
     * Validates that a service can be provided.
     *
     * When all is well an empty list is returned.
     * Otherwise a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    /**
     * Provides an already existing object as a service
     */
    data class ForInstance internal constructor(
        override val type: Type,
        override val definition: ServiceDefinition,
        private val instance: Any,
    ) : ServiceProvider {

        /**
         * The list of created instance always holds the [instance]
         */
        override val instances = listOf(
            CreatedInstance(instance, Instant.now())
        )

        /**
         * Simply returns the [instance]
         */
        override fun provide(context: InjectionContext) = instance

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer) = listOf<String>()
    }

    /**
     * Provides a global singleton service
     */
    data class ForSingleton internal constructor(
        override val type: Type,
        override val definition: ServiceDefinition
    ) : ServiceProvider {

        /**
         * True when the service instance was created
         */
        override val instances = mutableListOf<CreatedInstance>()

        private var instance: Any? = null

        /**
         * Get or create the instance of the service
         */
        override fun provide(context: InjectionContext): Any = instance ?: synchronized(this) {
            instance ?: create(context).apply {
                instance = this
                instances.add(CreatedInstance(this, Instant.now()))
            }
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.paramProviders.flatMap {
            it.validate(kontainer)
        }

        /**
         * Creates a new instance
         */
        private fun create(context: InjectionContext): Any = definition.producer.creator(
            context.kontainer,
            definition.producer.paramProviders.map {
                it.provide(
                    context.next(definition.serviceCls)
                )
            }.toTypedArray()
        )
    }

    /**
     * Provides a prototype service
     *
     * Each call to [provide] will create a new instance.
     */
    data class ForPrototype internal constructor(
        override val definition: ServiceDefinition
    ) : ServiceProvider {

        class Provider(private val definition: ServiceDefinition) : ServiceProvider.Provider {

            override fun provide(): ForPrototype {
                return ForPrototype(definition = definition)
            }
        }

        /**
         * The type is always prototype
         */
        override val type: Type = Type.Prototype

        /**
         * True when the service instance was created
         */
        override val instances = mutableListOf<CreatedInstance>()

        /**
         * Get or create the instance of the service
         */
        override fun provide(context: InjectionContext): Any = synchronized(this) {
            create(context).apply {
                instances.add(CreatedInstance(this, Instant.now()))
            }
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(kontainer: Kontainer) = definition.producer.paramProviders.flatMap {
            it.validate(kontainer)
        }

        /**
         * Creates a new instance
         */
        private fun create(context: InjectionContext): Any = definition.producer.creator(
            context.kontainer,
            definition.producer.paramProviders.map {
                it.provide(
                    context.next(definition.serviceCls)
                )
            }.toTypedArray()
        )
    }
}
