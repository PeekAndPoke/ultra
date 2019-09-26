package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.common.SimpleLazy
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * ParameterProviders provide parameters needed to instantiate services.
 */
interface ParameterProvider {

    /**
     * Provides the parameter value
     */
    fun provide(container: Kontainer): Any

    /**
     * Validates that a parameter can be provided
     *
     * When all is well an empty list is returned.
     * Otherwise a list of error strings is returned.
     */
    fun validate(container: Kontainer): List<String>

    companion object Factory {

        /**
         * Factory
         */
        fun of(parameter: KParameter): ParameterProvider {

            val paramCls = parameter.type.classifier as KClass<*>

            return when {
                // Config values: primitive types or strings are config values
                isPrimitive(paramCls) -> ForConfigValue(parameter)

                // Lazy<List<T>>: lazily injects all super types of T
                isLazyListType(parameter.type) -> ForLazyListOfServices(parameter)

                // Lazy<T>: lazily inject a service
                isLazyServiceType(parameter.type) -> ForLazyService(parameter)

                // List<T>: injects all super types of T
                isListType(parameter.type) -> ForListOfServices(parameter)

                // Lookup<T>: injects all super types of T as a lookup
                isLookupType(parameter.type) -> ForLookupOfServices(parameter)

                // Service: when there are no type parameters we have a usual service class
                isServiceType(paramCls) -> ForService(parameter)

                // otherwise we cannot handle it
                else -> UnknownInjection(parameter)
            }
        }
    }

    /**
     * Provider for configuration values
     */
    data class ForConfigValue internal constructor(private val parameter: KParameter) : ParameterProvider {

        private val paramCls by lazy { parameter.type.classifier as KClass<*> }

        private val paramName by lazy { parameter.name!! }

        override fun provide(container: Kontainer) = container.getConfig<Any>(paramName)

        override fun validate(container: Kontainer) = when {

            container.hasConfig(paramName, paramCls) -> listOf()

            else -> listOf("Parameter '${paramName}' misses a config value '${paramName}' of type ${paramCls.qualifiedName}")
        }
    }

    /**
     * Base for single service providers
     */
    abstract class ForServiceBase internal constructor(private val parameter: KParameter) {

        abstract val paramCls: KClass<*>

        private val paramName by lazy { parameter.name!! }

        fun validate(container: Kontainer) = container.getCandidates(paramCls).let {

            when {
                // When there is exactly one candidate everything is fine
                it.size == 1 -> listOf()

                // When there is no candidate then we cannot satisfy the dependency
                it.isEmpty() -> listOf("Parameter '${paramName}' misses a dependency to '${paramCls.qualifiedName}'")

                // When there is more than one candidate we cannot distinctly satisfy the dependency
                else -> listOf(
                    "Parameter '${paramName}' is ambiguous. The following services collide: " +
                            it.map { c -> c.qualifiedName }.joinToString(", ")
                )
            }
        }
    }

    /**
     * Provider for a single object
     */
    class ForService internal constructor(parameter: KParameter) : ForServiceBase(parameter), ParameterProvider {

        override val paramCls by lazy { parameter.type.classifier as KClass<*> }

        override fun provide(container: Kontainer) = container.get(paramCls)
    }

    /**
     * Provider for list of services
     */
    data class ForListOfServices internal constructor(private val parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = getInnerClass(parameter.type)

        /**
         * Provides a list with all super types
         */
        override fun provide(container: Kontainer): List<Any> = container.getAll(innerType)

        /**
         * Always valid.
         */
        override fun validate(container: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lookup of services
     */
    data class ForLookupOfServices internal constructor(private val parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = getInnerClass(parameter.type)

        /**
         * Provides a list with all super types
         */
        override fun provide(container: Kontainer): Lookup<out Any> = container.getLookup(innerType)

        /**
         * Always valid.
         */
        override fun validate(container: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy list of services
     */
    class ForLazyListOfServices internal constructor(parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list within the lazy
         */
        private val innerType = getInnerInnerClass(parameter.type)

        /**
         * Provides a lazy list with all super types
         */
        override fun provide(container: Kontainer): Lazy<List<Any>> = SimpleLazy { container.getAll(innerType) }

        /**
         * Always valid.
         */
        override fun validate(container: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy service
     */
    class ForLazyService internal constructor(parameter: KParameter) : ForServiceBase(parameter), ParameterProvider {

        override val paramCls by lazy { parameter.type.arguments[0].type!!.classifier as KClass<*> }

        override fun provide(container: Kontainer) = SimpleLazy { container.get(paramCls) }
    }

    /**
     * Fallback that always produces an error
     */
    data class UnknownInjection internal constructor(private val parameter: KParameter) : ParameterProvider {

        private val paramCls = parameter.type.classifier as KClass<*>

        private val error = "Parameter '${parameter.name}' has no known way to inject a '${paramCls.qualifiedName}'"

        override fun provide(container: Kontainer) = error(error)

        override fun validate(container: Kontainer) = listOf(error)
    }
}
