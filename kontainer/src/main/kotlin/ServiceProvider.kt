package de.peekandpoke.ultra.kontainer

/**
 * Base for all service providers
 */
interface ServiceProvider {

    enum class Type {
        GlobalSingleton,
        SemiDynamic,
        Dynamic
    }

    /**
     * The type of the service that is created
     */
    val type: Type

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
    data class ForInstance(override val type: Type, private val instance: Any) : ServiceProvider {

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
    data class ForSingleton(
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

        private var instance: Any? = null

        /**
         * Get or create the instance of the service
         */
        override fun provide(container: Kontainer): Any = instance ?: create(container).apply { instance = this }

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


