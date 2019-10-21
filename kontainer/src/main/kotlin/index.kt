package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Creates a kontainer blueprint
 */
fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint =
    KontainerBuilder(builder).buildBlueprint()

/**
 * Creates a kontainer module
 */
fun module(builder: KontainerBuilder.() -> Unit) = KontainerModule(builder)

/**
 * Creates a parameterized kontainer module
 */
fun <P> module(builder: KontainerBuilder.(P) -> Unit) = ParameterizedKontainerModule(builder)

/**
 * Kontainer module
 */
class KontainerModule(private val module: KontainerBuilder.() -> Unit) {
    fun apply(builder: KontainerBuilder) {
        builder.module()
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule<P>(private val module: KontainerBuilder.(P) -> Unit) {
    fun apply(builder: KontainerBuilder, param: P) {
        builder.module(param)
    }
}

/**
 * Defines a service
 *
 * Specifies which class the definitions [produces].
 * Specifies the [type] of the service.
 * The [ServiceProducer] is used to create an instance of the service
 */
class ServiceDefinition internal constructor(
    val produces: KClass<*>,
    val type: InjectionType,
    val producer: ServiceProducer
)

/**
 * Type of injection
 */
enum class InjectionType {
    /** A singleton service is shared across multiple kontainer instances */
    Singleton,
    /** For A prototype service a new instance is created whenever it is requested */
    Prototype,
    /** A dynamic service is a singleton that only lives within a single kontainer instance */
    Dynamic
}

