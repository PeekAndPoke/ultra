package de.peekandpoke.ultra.kontainer

import de.peekandpoke.ultra.kontainer.ParameterProvider.ProvisionType.Direct
import de.peekandpoke.ultra.kontainer.ParameterProvider.ProvisionType.Lazy
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.Lazy as KotlinLazy

/**
 * ParameterProviders provide parameters needed to instantiate services.
 */
interface ParameterProvider {

    companion object Factory {
        fun of(name: String, type: KType): ParameterProvider {
            val cls = type.classifier as KClass<*>

            return when {
                // InjectionContext: inject the InjectionContext
                cls.isInjectionContext() -> ForInjectionContext(name)

                // Lazy<List<T>>: lazily injects all super types of T
                type.isLazyListType() -> ForLazyListOfServices(name, type)

                // Lazy<T>: lazily inject a service
                type.isLazyServiceType() -> ForLazyService(name, type)

                // List<T>: injects all super types of T
                type.isListType() -> ForListOfServices(name, type)

                // Lookup<T>: injects all super types of T as a lookup
                type.isLookupType() -> ForLookupOfServices(name, type)

                // Service: when there are no type parameters we have a usual service class
                cls.isServiceType() -> ForService(name, type)

                // Otherwise, we cannot handle it
                else -> UnknownInjection(name, type)
            }
        }

        fun of(name: String, cls: KClass<out Any>, nullable: Boolean): ParameterProvider {
            return of(name, cls.createType(nullable = nullable))
        }

        fun of(parameter: KParameter): ParameterProvider {
            val name = parameter.name ?: "p${parameter.index}"
            val type = parameter.type

            return of(name, type)
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
    val name: String

    /**
     * The type of the parameter
     */
    val type: KType

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
    fun provide(kontainer: Kontainer, context: InjectionContext): Any?

    /**
     * Validates that a parameter can be provided
     *
     * When all is well an empty list is returned.
     * Otherwise, a list of error strings is returned.
     */
    fun validate(kontainer: Kontainer): List<String>

    /**
     * Provider for the injection context
     */
    class ForInjectionContext internal constructor(
        override val name: String,
    ) : ParameterProvider {

        override val type: KType get() = InjectionContext::class.createType()

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> =
            setOf(InjectionContext::class)

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Injects the context
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): InjectionContext {
            return context
        }

        /**
         * Always valid
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Base for single service providers
     */
    class ForService internal constructor(
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        private val isNullable = type.isMarkedNullable
        private val cls = type.classifier as KClass<*>

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> =
            setOf(cls)

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Direct

        /**
         * Provides the service
         *
         * NOTICE: we always use context.getOrNull(). Validity is checked in [validate]
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): Any? {
            return kontainer.getOrNull(cls, context)
        }

        /**
         * Validates if the service can be provided by the container
         */
        override fun validate(kontainer: Kontainer): List<String> = when {

            isNullable -> listOf()

            else -> kontainer.getCandidates(cls).let {

                when {
                    // When there is exactly one candidate everything is fine
                    it.size == 1 -> listOf()

                    // When there is no candidate then we cannot satisfy the dependency
                    it.isEmpty() -> listOf(
                        "Parameter '$name' misses a dependency to ${cls.getName()}"
                    )

                    // When there is more than one candidate we cannot distinctly satisfy the dependency
                    else -> listOf(
                        "Parameter '$name' is ambiguous. The following services collide: " +
                                it.joinToString(", ") { c -> c.getName() }
                    )
                }
            }
        }
    }

    /**
     * Provider for list of services
     */
    @ConsistentCopyVisibility
    data class ForListOfServices internal constructor(
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = type.getInnerClass()

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
        override fun provide(kontainer: Kontainer, context: InjectionContext): List<Any> {
            return kontainer.getAll(innerType, context)
        }

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lookup of services
     */
    @ConsistentCopyVisibility
    data class ForLookupOfServices internal constructor(
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list
         */
        private val innerType = type.getInnerClass()

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
        override fun provide(kontainer: Kontainer, context: InjectionContext): LazyServiceLookup<out Any> {
            return kontainer.getLookup(innerType, context)
        }

        /**
         * Always valid.
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf()
    }

    /**
     * Provider for a lazy list of services
     */
    class ForLazyListOfServices internal constructor(
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        /**
         * Get the type parameter of the list within the lazy
         */
        private val innerType = type.getInnerInnerClass()

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
        override fun provide(kontainer: Kontainer, context: InjectionContext): KotlinLazy<List<Any>> {
            return lazy {
                kontainer.getAll(innerType, context)
            }
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
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        /** The type wrapped within the Lazy */
        private val innerType = type.arguments[0].type!!

        /** The class of the service to be injected */
        private val paramCls = innerType.classifier as KClass<*>

        /** 'true" when the parameter is nullable, meaning that the service is optional */
        private val isNullable = innerType.isMarkedNullable

        /**
         * Get the injected services types
         */
        override fun getInjectedServiceTypes(blueprint: KontainerBlueprint): Set<KClass<*>> =
            setOf(paramCls)

        /**
         * Gets the provision type of the parameter.
         */
        override fun getProvisionType(): ProvisionType = Lazy

        /**
         * Provides the service wrapped as a Lazy
         */
        override fun provide(kontainer: Kontainer, context: InjectionContext): KotlinLazy<Any?> {
            return lazy {
                kontainer.getOrNull(paramCls, context)
            }
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
                        "Parameter '$name' misses a lazy dependency to '${paramCls.getName()}'"
                    )

                    // When there is more than one candidate we cannot distinctly satisfy the dependency
                    else -> listOf(
                        "Parameter '$name' is ambiguous. The following services collide: " +
                                it.joinToString(", ") { c -> c.getName() }
                    )
                }
            }
        }
    }

    /**
     * Fallback that always produces an error
     */
    @ConsistentCopyVisibility
    data class UnknownInjection internal constructor(
        override val name: String,
        override val type: KType,
    ) : ParameterProvider {

        /** The class of the service to be injected */
        private val paramCls = type.classifier as KClass<*>

        /** The error that will be raised by [provide] and [validate] */
        private val error = "Parameter '${name}' has no known way to inject a ${paramCls.getName()}"

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
        override fun provide(kontainer: Kontainer, context: InjectionContext): Nothing {
            throw KontainerInconsistent(error)
        }

        /**
         * Will always return an error
         */
        override fun validate(kontainer: Kontainer): List<String> = listOf(error)
    }
}
