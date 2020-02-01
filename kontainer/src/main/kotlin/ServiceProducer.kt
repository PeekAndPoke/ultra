package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.reflect

/**
 * Produce service instance
 *
 * The [signature] is the list of parameters of the primary constructor of the service.
 *
 * The [creator] is a function that produces the service instance and expects the correct parameters to do so.
 */
class ServiceProducer internal constructor(
    val signature: List<KParameter>,
    val creator: (kontainer: Kontainer, params: Array<Any?>) -> Any
) {
    val paramProviders = signature.map { ParameterProvider.of(it) }

    companion object {

        /**
         * Creates a producer for the Kontainer itself, making it possible to inject the Kontainer.
         */
        fun forKontainer() = ServiceProducer(listOf()) { kontainer, _ -> kontainer }

        /**
         * Creates a producer for the KontainerBlueprint, making it possible to inject the KontainerBlueprint that
         * produced the Kontainer.
         */
        fun forKontainerBlueprint() = ServiceProducer(listOf()) { kontainer, _ -> kontainer.blueprint }

        /**
         * Creates a producer for an already existing service instance.
         */
        fun forInstance(instance: Any) = ServiceProducer(listOf()) { _, _ -> instance }

        /**
         * Creates a producer for the given class.
         */
        fun forClass(cls: KClass<*>): ServiceProducer {

            if (cls.java.isInterface || cls.isAbstract) {
                throw InvalidClassProvided("A service cannot be an interface or abstract class")
            }

            return ServiceProducer(cls.primaryConstructor!!.parameters) { _, params ->
                cls.primaryConstructor!!.call(*params)
            }
        }

        /**
         * Creates a producer for a factory method with zero parameters.
         */
        fun <R : Any> forFactory(factory: () -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }

        /**
         * Creates a producer for a factory method with one parameter.
         */
        fun <R : Any, P1> forFactory(factory: (P1) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1)
            }

        /**
         * Creates a producer for a factory method with two parameters.
         */
        fun <R : Any, P1, P2> forFactory(factory: (P1, P2) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2)
            }

        /**
         * Creates a producer for a factory method with three parameters.
         */
        fun <R : Any, P1, P2, P3> forFactory(factory: (P1, P2, P3) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }

        /**
         * Creates a producer for a factory method with four parameters.
         */
        fun <R : Any, P1, P2, P3, P4> forFactory(factory: (P1, P2, P3, P4) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }

        /**
         * Creates a producer for a factory method with five parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5> forFactory(factory: (P1, P2, P3, P4, P5) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }

        /**
         * Creates a producer for a factory method with six parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6> forFactory(factory: (P1, P2, P3, P4, P5, P6) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }

        /**
         * Creates a producer for a factory method with seven parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7> forFactory(factory: (P1, P2, P3, P4, P5, P6, P7) -> R) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }
    }
}
