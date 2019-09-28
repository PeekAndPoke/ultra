package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * Creates a kontainer
 */
fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint =
    KontainerBuilder(builder).buildBlueprint()

/**
 * Creates a kontainer module
 */
fun module(builder: KontainerBuilder.() -> Unit): KontainerModule =
    KontainerBuilder(builder).buildModule()

data class KontainerModule internal constructor(
    val config: Map<String, Any>,
    val definitions: Map<KClass<*>, ServiceDefinition>,
    val definitionLocations: Map<KClass<*>, StackTraceElement>
)

data class ServiceDefinition internal constructor(
    val produces: KClass<*>,
    val type: InjectionType,
    val producer: Producer? = null
)

enum class InjectionType {
    Singleton,
    Dynamic
}

data class Producer internal constructor(
    val signature: List<KParameter>,
    val creator: (kontainer: Kontainer, params: Array<Any>) -> Any
)
