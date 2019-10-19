package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.reflect

/**
 * Creates a kontainer
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


class ServiceDefinition internal constructor(
    val produces: KClass<*>,
    val type: InjectionType,
    val producer: Producer
)

enum class InjectionType {
    Singleton,
    Prototype,
    Dynamic
}

class Producer internal constructor(
    signature: List<KParameter>,
    val creator: (kontainer: Kontainer, params: Array<Any?>) -> Any
) {
    val paramProviders = signature.map { ParameterProvider.of(it) }

    companion object {

        fun forKontainer() = Producer(listOf()) { kontainer, _ -> kontainer }

        fun forKontainerBlueprint() = Producer(listOf()) { kontainer, _ -> kontainer.blueprint }

        fun forInstance(instance: Any) = Producer(listOf()) { _, _ -> instance }

        fun forClass(cls: KClass<*>): Producer {

            if (cls.java.isInterface || cls.isAbstract) {
                throw InvalidClassProvided("A service cannot be an interface or abstract class")
            }

            return Producer(cls.primaryConstructor!!.parameters) { _, params ->
                cls.primaryConstructor!!.call(*params)
            }
        }

        fun <R : Any> forFactory(factory: () -> R) =
            Producer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }

        fun <R : Any, P1> forFactory(factory: (P1) -> R) =
            Producer(factory.reflect()!!.parameters) { _, (p1) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1)
            }

        fun <R : Any, P1, P2> forFactory(factory: (P1, P2) -> R) =
            Producer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2)
            }

        fun <R : Any, P1, P2, P3> forFactory(factory: (P1, P2, P3) -> R) =
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }

        fun <R : Any, P1, P2, P3, P4> forFactory(factory: (P1, P2, P3, P4) -> R) =
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }

        fun <R : Any, P1, P2, P3, P4, P5> forFactory(factory: (P1, P2, P3, P4, P5) -> R) =
            Producer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }

        fun <R : Any, P1, P2, P3, P4, P5, P6> forFactory(factory: (P1, P2, P3, P4, P5, P6) -> R) =
            Producer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }

        fun <R : Any, P1, P2, P3, P4, P5, P6, P7> forFactory(factory: (P1, P2, P3, P4, P5, P6, P7) -> R) =
            Producer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }
    }
}
