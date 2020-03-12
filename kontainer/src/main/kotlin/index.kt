package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Creates a kontainer blueprint
 */
fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint =
    KontainerBuilder(builder).build()

/**
 * Creates a kontainer module
 */
fun module(builder: KontainerBuilder.() -> Unit) = KontainerModule(builder)

/**
 * Creates a parameterized kontainer module
 */
fun <P> module(builder: KontainerBuilder.(P) -> Unit) = ParameterizedKontainerModule(builder)

/**
 * Creates a parameterized kontainer module with two parameters
 */
fun <P1, P2> module(builder: KontainerBuilder.(P1, P2) -> Unit) = ParameterizedKontainerModule2(builder)

/**
 * Creates a parameterized kontainer module with three parameters
 */
fun <P1, P2, P3> module(builder: KontainerBuilder.(P1, P2, P3) -> Unit) = ParameterizedKontainerModule3(builder)

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
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule2<P1, P2>(private val module: KontainerBuilder.(P1, P2) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2) {
        builder.module(p1, p2)
    }
}

/**
 * Parameterized Kontainer module
 */
class ParameterizedKontainerModule3<P1, P2, P3>(private val module: KontainerBuilder.(P1, P2, P3) -> Unit) {
    fun apply(builder: KontainerBuilder, p1: P1, p2: P2, p3: P3) {
        builder.module(p1, p2, p3)
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
) {
    fun withType(type: InjectionType) = ServiceDefinition(this.produces, type, this.producer)
}

/**
 * Type of injection
 */
enum class InjectionType {
    /** A singleton service is shared across multiple kontainer instances */
    Singleton,
    /** For a prototype service a new instance is created whenever it is injected */
    Prototype,
    /** A dynamic service is a singleton that only lives within a single kontainer instance */
    Dynamic
}

