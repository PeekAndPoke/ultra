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
    fun provide(context: InjectionContext): Any?

    /**
     * Validates that a parameter can be provided
     *
     * When all is well an empty list is returned.
     * Otherwise a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    companion object Factory {

        /**
         * Factory
         */
        fun of(parameter: KParameter): ParameterProvider {

            val paramCls = parameter.type.classifier as KClass<*>

            return when {
                // Config values: primitive types or strings are config values
                paramCls.isPrimitiveOrString() -> ForConfigValue(parameter)

                // InjectionContext: inject the InjectionContext
                paramCls.isInjectionContext() -> ForInjectionContext()

                // Lazy<List<T>>: lazily injects all super types of T
                parameter.type.isLazyListType() -> ForLazyListOfServices(parameter)

                // Lazy<T>: lazily inject a service
                parameter.type.isLazyServiceType() -> ForLazyService(parameter)

                // List<T>: injects all super types of T
                parameter.type.isListType() -> ForListOfServices(parameter)

                // Lookup<T>: injects all super types of T as a lookup
                parameter.type.isLookupType() -> ForLookupOfServices(parameter)

                // Service: when there are no type parameters we have a usual service class
                paramCls.isServiceType() -> ForService(parameter)

                // otherwise we cannot handle it
                else -> UnknownInjection(parameter)
            }
        }
    }

    /**
     * Provider for configuration values
     */
    data class ForConfigValue internal constructor(private val parameter: KParameter) : ParameterProvider {

        private val paramCls = parameter.type.classifier as KClass<*>

        private val paramName = parameter.name ?: "n/a"

        override fun provide(context: InjectionContext) = context.getConfig<Any>(paramName)

        override fun validate(kontainer: Kontainer) = when {

            kontainer.hasConfig(paramName, paramCls) -> listOf()

            else -> listOf("Parameter '${paramName}' misses a config value '${paramName}' of type ${paramCls.qualifiedName}")
        }
    }

    /**
     * Provider for the injection context
     */
    class ForInjectionContext internal constructor() : ParameterProvider {

        /**
         * Injects the context
         */
        override fun provide(context: InjectionContext) = context

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Base for single service providers
     */
    abstract class ForServiceBase internal constructor(private val parameter: KParameter) : ParameterProvider {

        abstract val paramCls: KClass<*>

        private val paramName = parameter.name ?: "n/a"

        override fun validate(kontainer: Kontainer) = when {

            parameter.type.isMarkedNullable -> listOf()

            else -> kontainer.getCandidates(paramCls).let {

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

        override fun provide(context: InjectionContext) = when {

            parameter.type.isMarkedNullable -> context.getOrNull(paramCls)

            else -> context.get(paramCls)
        }
    }

    /**
     * Provider for a single object
     */
    class ForService internal constructor(parameter: KParameter) : ForServiceBase(parameter) {

        override val paramCls = parameter.type.classifier as KClass<*>
    }

    /**
     * Provider for list of services
     */
    data class ForListOfServices internal constructor(private val parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = parameter.type.getInnerClass()

        /**
         * Provides a list with all super types
         */
        override fun provide(context: InjectionContext): List<Any> = context.getAll(innerType)

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lookup of services
     */
    data class ForLookupOfServices internal constructor(private val parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = parameter.type.getInnerClass()

        /**
         * Provides a list with all super types
         */
        override fun provide(context: InjectionContext): Lookup<out Any> = context.getLookup(innerType)

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy list of services
     */
    class ForLazyListOfServices internal constructor(parameter: KParameter) : ParameterProvider {

        /**
         * Get the type parameter of the list within the lazy
         */
        private val innerType = parameter.type.getInnerInnerClass()

        /**
         * Provides a lazy list with all super types
         */
        override fun provide(context: InjectionContext): Lazy<List<Any>> = SimpleLazy { context.getAll(innerType) }

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy service
     */
    class ForLazyService internal constructor(parameter: KParameter) : ParameterProvider {

        private val innerType = parameter.type.arguments[0].type!!

        private val innerCls = innerType.classifier as KClass<*>

        private val paramName = parameter.name

        override fun provide(context: InjectionContext) = SimpleLazy { context.getOrNull(innerCls) }

        override fun validate(kontainer: Kontainer) = when {

            innerType.isMarkedNullable -> listOf()

            else -> kontainer.getCandidates(innerCls).let {

                when {
                    // When there is exactly one candidate everything is fine
                    it.size == 1 -> listOf()

                    // When there is no candidate then we cannot satisfy the dependency
                    it.isEmpty() -> listOf(
                        "Parameter '${paramName}' misses a lazy dependency to '${innerCls.qualifiedName}'"
                    )

                    // When there is more than one candidate we cannot distinctly satisfy the dependency
                    else -> listOf(
                        "Parameter '${paramName}' is ambiguous. The following services collide: " +
                                it.map { c -> c.qualifiedName }.joinToString(", ")
                    )
                }
            }
        }
    }

    /**
     * Fallback that always produces an error
     */
    data class UnknownInjection internal constructor(private val parameter: KParameter) : ParameterProvider {

        private val paramCls = parameter.type.classifier as KClass<*>

        private val error = "Parameter '${parameter.name}' has no known way to inject a '${paramCls.qualifiedName}'"

        override fun provide(context: InjectionContext) = error(error)

        override fun validate(kontainer: Kontainer) = listOf(error)
    }
}
