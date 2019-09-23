package de.peekandpoke.ultra.kontainer

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

    data class ForConfigValue(private val parameter: KParameter) : ParameterProvider {

        private val paramCls by lazy { parameter.type.classifier as KClass<*> }

        private val paramName by lazy { parameter.name!! }

        override fun provide(container: Kontainer) = container.getConfig<Any>(paramName)

        override fun validate(container: Kontainer) = when {

            container.hasConfig(paramName, paramCls) -> listOf()

            else -> listOf("Parameter '${paramName}' misses a config value '${paramName}' of type ${paramCls.qualifiedName}")
        }
    }

    /**
     * Parameter provider for a single object
     */
    data class ForSimpleObject(private val parameter: KParameter) : ParameterProvider {

        private val paramCls by lazy { parameter.type.classifier as KClass<*> }

        override fun provide(container: Kontainer) = container.get(paramCls)

        override fun validate(container: Kontainer) = container.getCandidates(paramCls).let {

            when {
                // When there is exactly one candidate everything is fine
                it.size == 1 -> listOf()

                // When there is no candidate then we cannot satisfy the dependency
                it.isEmpty() -> listOf("Parameter '${parameter.name}' misses a dependency to '${paramCls.qualifiedName}'")

                // When there is more than one candidate we cannot distinctly satisfy the dependency
                else -> listOf(
                    "Parameter '${parameter.name}' is ambiguous. The following services collide: " +
                            it.map { c -> c.qualifiedName }.joinToString(", ")
                )
            }
        }
    }

    /**
     * Fallback that always produces an error
     */
    data class UnknownInjection(private val parameter: KParameter) : ParameterProvider {

        private val paramCls = parameter.type.classifier as KClass<*>

        private val error = "Parameter '${parameter.name}' has no known way to inject a '${paramCls.qualifiedName}'"

        override fun provide(container: Kontainer) = error(error)

        override fun validate(container: Kontainer) = listOf(error)
    }

    companion object {
        fun of(parameter: KParameter): ParameterProvider {

            val paramCls = parameter.type.classifier as KClass<*>

            return when {

                paramCls.java.isPrimitive || paramCls == String::class -> ForConfigValue(parameter)

                paramCls.typeParameters.isEmpty() -> ForSimpleObject(parameter)

                else -> UnknownInjection(parameter)
            }
        }
    }
}
