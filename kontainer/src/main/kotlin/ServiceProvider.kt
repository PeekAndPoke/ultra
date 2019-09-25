package de.peekandpoke.ultra.kontainer

import java.time.Instant

/**
 * Base for all service providers
 */
interface ServiceProvider {

    enum class Type {
        GlobalSingleton,
        SemiDynamic,
        Dynamic,
        DynamicDefault,
    }

    /**
     * The type of the service that is created
     */
    val type: Type

    /**
     * True when the service is already created
     */
    val createdAt: Instant?

    /**
     * Provides the service instance
     */
    fun provide(container: Kontainer): Any

    /**
     * Validates that a service can be provided.
     *
     * When all is well an empty list is returned.
     * Otherwise a list of error strings is returned.
     */
    fun validate(container: Kontainer): List<String>

    /**
     * Provides an already existing object as a service
     */
    data class ForInstance internal constructor(override val type: Type, private val instance: Any) : ServiceProvider {

        /**
         * Always true
         */
        override val createdAt: Instant = Instant.now()

        /**
         * Simply returns the [instance]
         */
        override fun provide(container: Kontainer) = instance

        /**
         * Always valid
         */
        override fun validate(container: Kontainer) = listOf<String>()
    }

    /**
     * Provides a singleton service
     *
     * The [creator] is the function that will create the service
     *
     * The [paramProviders] create the parameters passed to [creator]
     */
    data class ForSingleton internal constructor(
        override val type: Type,
        val creator: (Array<Any>) -> Any,
        val paramProviders: List<ParameterProvider>
    ) : ServiceProvider {

        companion object {
            fun of(type: Type, definition: ServiceDefinition) = ForSingleton(
                type,
                definition.producer!!.creator,
                definition.producer.signature.map { ParameterProvider.of(it) }
            )
        }

        /**
         * True when the service instance was created
         */
        override var createdAt: Instant? = null

        private var instance: Any? = null

        /**
         * Get or create the instance of the service
         */
        override fun provide(container: Kontainer): Any = instance ?: create(container).apply {
            createdAt = Instant.now()
            instance = this
        }

        /**
         * Validates that all parameters can be provided
         */
        override fun validate(container: Kontainer) = paramProviders.flatMap {
            it.validate(container)
        }

        /**
         * Creates a new instance
         */
        private fun create(container: Kontainer): Any = creator(
            paramProviders.map { it.provide(container) }.toTypedArray()
        )
    }
}


