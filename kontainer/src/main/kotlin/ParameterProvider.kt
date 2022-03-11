package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.kontainer.ParameterProvider.ProvisionType.Direct
import de.peekandpoke.ultra.kontainer.ParameterProvider.ProvisionType.Lazy
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.Lazy as KotlinLazy

/**
 * ParameterProviders provide parameters needed to instantiate services.
 */
interface ParameterProvider {

    companion object Factory {
        /** Factory method */
        fun of(parameter: KParameter): ParameterProvider {

            val paramCls = parameter.type.classifier as KClass<*>

            return when {
                // Config values: primitive types or strings are config values
                paramCls.isPrimitiveOrString() -> ForConfigValue(parameter)

                // InjectionContext: inject the InjectionContext
                paramCls.isInjectionContext() -> ForInjectionContext(parameter)

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

                // Otherwise, we cannot handle it
                else -> UnknownInjection(parameter)
            }
        }
    }

    /**
     * Defines how a services are provided.
     *
     * [Direct] .. the service instances is provided directly.
     * [Lazy] .. the service instances are provided lazily.
     */
    enum class ProvisionType {
        Direct,
        Lazy,
    }

    /**
     * The name of the parameter
     */
    val parameter: KParameter

    /**
     * Get the injected services types
     */
    fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>>

    /**
     * Gets the provision type of the parameter.
     */
    fun getProvisionType(): ProvisionType

    /**
     * Provides the parameter value
     */
    fun provide(context: InjectionContext): Any?

    /**
     * Validates that a parameter can be provided
     *
     * When all is well an empty list is returned.
     * Otherwise, a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    /**
     * Provider for configuration values
     */
    data class ForConfigValue internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /** The type of the config value to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** The name of the parameter for which the config value is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> = setOf(
            paramCls,
        )

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Provides the config value
         */
        override fun provide(context: InjectionContext): Any = context.getConfig(paramName)

        /**
         * Returns 'true" when a requested injection can be fulfilled.
         */
        override fun validate(kontainer: Kontainer): List<String> = when {

            kontainer.hasConfig(paramName, paramCls) -> listOf()

            else -> listOf(
                "Parameter '$paramName' misses a config value '$paramName' of type ${paramCls.qualifiedName}"
            )
        }
    }

    /**
     * Provider for the injection context
     */
    class ForInjectionContext internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> = setOf(
            InjectionContext::class,
        )

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Injects the context
         */
        override fun provide(context: InjectionContext): InjectionContext = context

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Base for single service providers
     */
    class ForService internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /** The class of the service to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** 'true" when the parameter is nullable, meaning that the service is optional */
        private val isNullable = parameter.type.isMarkedNullable

        /** The name of the parameter for which the service is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> = setOf(
            paramCls,
        )

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Provides the service
         *
         * NOTICE: we always use context.getOrNull(). Validity is checked in [validate]
         */
        override fun provide(context: InjectionContext): Any? = context.getOrNull(paramCls)

        /**
         * Validates if the service can be provided by the container
         */
        override fun validate(kontainer: Kontainer): List<String> = when {

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
    data class ForListOfServices internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = parameter.type.getInnerClass()

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> {
            return blueprint.superTypeLookup.getAllCandidatesFor(innerType)
        }

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

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
    data class ForLookupOfServices internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = parameter.type.getInnerClass()

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> {
            return blueprint.superTypeLookup.getLookupBlueprint(innerType).getClasses()
        }

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Lazy

        /**
         * Provides a list with all super types
         */
        override fun provide(context: InjectionContext): LazyServiceLookup<out Any> = context.getLookup(innerType)

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy list of services
     */
    class ForLazyListOfServices internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list within the lazy
         */
        private val innerType = parameter.type.getInnerInnerClass()

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> {
            return blueprint.superTypeLookup.getAllCandidatesFor(innerType)
        }

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Lazy

        /**
         * Provides a lazy list with all super types
         */
        override fun provide(context: InjectionContext): KotlinLazy<List<Any>> = lazy {
            context.getAll(innerType)
        }

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy service
     */
    class ForLazyService internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /** The type wrapped within the Lazy */
        private val innerType = parameter.type.arguments[0].type!!

        /** The class of the service to be injected */
        private val paramCls = innerType.classifier as KClass<*>

        /** 'true" when the parameter is nullable, meaning that the service is optional */
        private val isNullable = innerType.isMarkedNullable

        /** The name of the parameter for which the service is to be injected */
        private val paramName = parameter.name ?: "n/a"

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> = setOf(
            paramCls,
        )

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Lazy

        /**
         * Provides the service wrapped as a Lazy
         */
        override fun provide(context: InjectionContext): KotlinLazy<Any?> = lazy {
            context.getOrNull(paramCls)
        }

        /**
         * Validates if the service can be provided by the container
         */
        override fun validate(kontainer: Kontainer): List<String> = when {

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
    data class UnknownInjection internal constructor(
        override val parameter: KParameter,
    ) : ParameterProvider {

        /** The class of the service to be injected */
        private val paramCls = parameter.type.classifier as KClass<*>

        /** The error that will be raised by [provide] and [validate] */
        private val error = "Parameter '${parameter.name}' has no known way to inject a '${paramCls.qualifiedName}'"

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> = emptySet()

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Will always raise a [KontainerInconsistent]
         */
        override fun provide(context: InjectionContext): Nothing = throw KontainerInconsistent(error)

        /**
         * Will always return an error
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf(error)
    }
}
