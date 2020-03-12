package de.peekandpoke.ultra.kontainer

import java.time.Instant

/**
 * Service providers create instances of services
 */
interface ServiceProvider {

    enum class Type {
        Singleton,
        Prototype,
        SemiDynamic,
        Dynamic,
        DynamicDefault,
    }

    data class CreatedInstance(
        val instance: Any,
        val createdAt: Instant
    )

    /**
     * The type of the service that is created
     */
    val type: Type

    /**
     * A list of all instances created by the provider
     */
    val instances: List<CreatedInstance>

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
    data class ForInstance internal constructor(override val type: Type, private val instance: Any) : ServiceProvider {

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
     * Provides a singleton service
     */
    data class ForSingleton internal constructor(
        override val type: Type,
        val definition: ServiceDefinition
    ) : ServiceProvider {

        /**
         * True when the service instance was created
         */
        override val instances = mutableListOf<CreatedInstance>()

        private var instance: Any? = null

        /**
         * Get or create the instance of the service
         */
        override fun provide(context: InjectionContext): Any = instance ?: create(context).apply {
            instance = this
            instances.add(
                CreatedInstance(this, Instant.now())
            )
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
                    context.next(definition.produces)
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
        val definition: ServiceDefinition
    ) : ServiceProvider {

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
        override fun provide(context: InjectionContext): Any = create(context).apply {
            instances.add(
                CreatedInstance(this, Instant.now())
            )
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
                    context.next(definition.produces)
                )
            }.toTypedArray()
        )
    }
}


