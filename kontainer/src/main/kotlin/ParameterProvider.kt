package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.common.Lookup
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

        /** The type of the config value to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** The name of the parameter for which the config value is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Provides the config value
         */
        override fun provide(context: InjectionContext) = context.getConfig<Any>(paramName)

        /**
         * Returns 'true" when a requested injection can be fulfilled.
         */
        override fun validate(kontainer: Kontainer) = when {

            kontainer.hasConfig(paramName, paramCls) -> listOf()

            else -> listOf(
                "Parameter '$paramName' misses a config value '$paramName' of type ${paramCls.qualifiedName}"
            )
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
    class ForService internal constructor(parameter: KParameter) : ParameterProvider {

        /** The class of the service to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** 'true" when the parameter is nullable, meaning that the service is optional */
        private val isNullable = parameter.type.isMarkedNullable

        /** The name of the parameter for which the service is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Provides the service
         *
         * NOTICE: we always use context.getOrNull(). Validity is checked in [validate]
         */
        override fun provide(context: InjectionContext) = context.getOrNull(paramCls)

        /**
         * Validates if the service can be provided by the container
         */
        override fun validate(kontainer: Kontainer) = when {

            isNullable -> listOf()

            else -> kontainer.getCandidates(paramCls).let {

                when {
                    // When there is exactly one candidate everything is fine
                    it.size == 1 -> listOf()

                    // When there is no candidate then we cannot satisfy the dependency
                    it.isEmpty() -> listOf(
                        "Parameter '$paramName' misses a dependency to '${paramCls.qualifiedName}'"
                    )

                    // When there is more than one candidate we cannot distinctly satisfy the dependency
                    else -> listOf(
                        "Parameter '$paramName' is ambiguous. The following services collide: " +
                                it.map { c -> c.qualifiedName }.joinToString(", ")
                    )
                }
            }
        }
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
        override fun provide(context: InjectionContext): Lazy<List<Any>> = lazy { context.getAll(innerType) }

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy service
     */
    class ForLazyService internal constructor(parameter: KParameter) : ParameterProvider {

        /** The type wrapped within the Lazy */
        private val innerType = parameter.type.arguments[0].type!!

        /** The class of the service to be injected */
        private val paramCls = innerType.classifier as KClass<*>

        /** 'true" when the parameter is nullable, meaning that the service is optional */
        private val isNullable = innerType.isMarkedNullable

        /** The name of the parameter for which the service is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Provides the service wrapped as a Lazy
         */
        override fun provide(context: InjectionContext): Lazy<Any?> = lazy {
            context.getOrNull(paramCls)
        }

        /**
         * Validates if the service can be provided by the container
         */
        override fun validate(kontainer: Kontainer) = when {

            isNullable -> listOf()

            else -> kontainer.getCandidates(paramCls).let {

                when {
                    // When there is exactly one candidate everything is fine
                    it.size == 1 -> listOf()

                    // When there is no candidate then we cannot satisfy the dependency
                    it.isEmpty() -> listOf(
                        "Parameter '$paramName' misses a lazy dependency to '${paramCls.qualifiedName}'"
                    )

                    // When there is more than one candidate we cannot distinctly satisfy the dependency
                    else -> listOf(
                        "Parameter '$paramName' is ambiguous. The following services collide: " +
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

        /** The class of the service to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** The error that will be raised by [provide] and [validate] */
        private val error = "Parameter '${parameter.name}' has no known way to inject a '${paramCls.qualifiedName}'"

        /**
         * Will always raise a [KontainerInconsistent]
         */
        override fun provide(context: InjectionContext) = throw KontainerInconsistent(error)

        /**
         * Will always return an error
         */
        override fun validate(kontainer: Kontainer) = listOf(error)
    }
}
