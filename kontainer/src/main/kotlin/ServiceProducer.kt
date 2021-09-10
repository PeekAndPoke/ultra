package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
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

    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    @OptIn(ExperimentalReflectionOnLambdas::class)
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

            val ctor = cls.primaryConstructor!!
            val ctorParams = ctor.parameters

            return when {
                ctorParams.isEmpty() -> ServiceProducer(ctorParams) { _, _ ->
                    cls.createInstance()
                }

                else -> ServiceProducer(ctor.parameters) { _, params ->
                    @Suppress("Detekt:SpreadOperator") // We can't avoid using the spread operator here for now
                    ctor.call(*params)
                }
            }
        }

        @Suppress("Detekt:ComplexMethod")
        fun <R : Any> forFactory(factory: Function<R>) = when (factory) {
            is Function0<R> -> forFactory(factory)
            is Function1<*, R> -> forFactory(factory)
            is Function2<*, *, R> -> forFactory(factory)
            is Function3<*, *, *, R> -> forFactory(factory)
            is Function4<*, *, *, *, R> -> forFactory(factory)
            is Function5<*, *, *, *, *, R> -> forFactory(factory)
            is Function6<*, *, *, *, *, *, R> -> forFactory(factory)
            is Function7<*, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function8<*, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function9<*, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function10<*, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function11<*, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function12<*, *, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)
            is Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, R> -> forFactory(factory)

            else -> throw InvalidServiceFactory("Currently only up to 10 injection parameters are supported")
        }

        /**
         * Creates a producer for a factory method with zero parameters.
         */
        fun <R : Any> forFactory(
            factory: () -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, _ ->
                factory.invoke()
            }

        /**
         * Creates a producer for a factory method with one parameter.
         */
        fun <R : Any, P1> forFactory(
            factory: (P1) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1)
            }

        /**
         * Creates a producer for a factory method with two parameters.
         */
        fun <R : Any, P1, P2> forFactory(
            factory: (P1, P2) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2)
            }

        /**
         * Creates a producer for a factory method with three parameters.
         */
        fun <R : Any, P1, P2, P3> forFactory(
            factory: (P1, P2, P3) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3)
            }

        /**
         * Creates a producer for a factory method with four parameters.
         */
        fun <R : Any, P1, P2, P3, P4> forFactory(
            factory: (P1, P2, P3, P4) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4)
            }

        /**
         * Creates a producer for a factory method with five parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5> forFactory(
            factory: (P1, P2, P3, P4, P5) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, (p1, p2, p3, p4, p5) ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p1 as P1, p2 as P2, p3 as P3, p4 as P4, p5 as P5)
            }

        /**
         * Creates a producer for a factory method with six parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6> forFactory(
            factory: (P1, P2, P3, P4, P5, P6) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6)
            }

        /**
         * Creates a producer for a factory method with seven parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(p[0] as P1, p[1] as P2, p[2] as P3, p[3] as P4, p[4] as P5, p[5] as P6, p[6] as P7)
            }

        /**
         * Creates a producer for a factory method with eight parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                )
            }

        /**
         * Creates a producer for a factory method with nine parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                )
            }

        /**
         * Creates a producer for a factory method with ten parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                )
            }

        /**
         * Creates a producer for a factory method with eleven parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                )
            }

        /**
         * Creates a producer for a factory method with twelve parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                    p[11] as P12,
                )
            }

        /**
         * Creates a producer for a factory method with thirteen parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                    p[11] as P12,
                    p[12] as P13,
                )
            }

        /**
         * Creates a producer for a factory method with fourteen parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                    p[11] as P12,
                    p[12] as P13,
                    p[13] as P14,
                )
            }

        /**
         * Creates a producer for a factory method with fifteen parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                    p[11] as P12,
                    p[12] as P13,
                    p[13] as P14,
                    p[14] as P15,
                )
            }

        /**
         * Creates a producer for a factory method with sixteen parameters.
         */
        fun <R : Any, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16> forFactory(
            factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R
        ) =
            ServiceProducer(factory.reflect()!!.parameters) { _, p ->
                @Suppress("UNCHECKED_CAST")
                factory.invoke(
                    p[0] as P1,
                    p[1] as P2,
                    p[2] as P3,
                    p[3] as P4,
                    p[4] as P5,
                    p[5] as P6,
                    p[6] as P7,
                    p[7] as P8,
                    p[8] as P9,
                    p[9] as P10,
                    p[10] as P11,
                    p[11] as P12,
                    p[12] as P13,
                    p[13] as P14,
                    p[14] as P15,
                    p[15] as P16,
                )
            }
    }
}
