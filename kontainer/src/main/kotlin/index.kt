package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

fun kontainer(builder: KontainerBuilder.() -> Unit): KontainerBlueprint = KontainerBuilder(builder).build()

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
    val creator: (Array<Any>) -> Any
)
