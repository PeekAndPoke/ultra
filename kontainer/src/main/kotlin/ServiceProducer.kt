package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/**
 * Produce service instance
 *
 * The [params] is the list of parameters of the primary constructor of the service.
 *
 * The [creator] is a function that produces the service instance and expects the correct parameters to do so.
 */
@Suppress("Detekt:TooManyFunctions")
class ServiceProducer<T : Any> internal constructor(
    val params: List<ParameterProvider>,
    val creates: KClass<out T>,
    val creator: (kontainer: Kontainer, params: Array<Any?>) -> T,
) {
    @Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
    companion object {
        /**
         * Creates a producer for the Kontainer itself, making it possible to inject the Kontainer.
         */
        fun forKontainer(): ServiceProducer<Kontainer> =
            ServiceProducer(listOf(), Kontainer::class) { kontainer, _ ->
                kontainer
            }

        /**
         * Creates a producer for the KontainerBlueprint, making it possible to inject the KontainerBlueprint that
         * produced the Kontainer.
         */
        fun forKontainerBlueprint(): ServiceProducer<KontainerBlueprint> =
            ServiceProducer(listOf(), KontainerBlueprint::class) { kontainer, _ ->
                kontainer.blueprint
            }

        /**
         * Creates a producer for an already existing service instance.
         */
        fun <T : Any> forInstance(instance: T): ServiceProducer<T> =
            ServiceProducer(listOf(), instance::class) { _, _ -> instance }

        /**
         * Creates a producer for the given class.
         */
        fun <T : Any> forClass(cls: KClass<T>): ServiceProducer<T> {

            if (cls.java.isInterface || cls.isAbstract) {
                throw InvalidClassProvided("A service cannot be an interface or abstract class")
            }

            val ctor = cls.primaryConstructor!!
            val ctorParams = ctor.parameters.map { ParameterProvider.of(it) }

            return when {
                ctorParams.isEmpty() -> ServiceProducer(emptyList(), cls) { _, _ ->
                    cls.createInstance()
                }

                else -> ServiceProducer(ctorParams, cls) { _, params ->
                    @Suppress("Detekt:SpreadOperator") // We can't avoid using the spread operator here for now
                    ctor.call(*params)
                }
            }
        }

        fun <R : Any> forFactory(
            params: List<KType>,
            creates: KClass<R>,
            factory: (Array<Any?>) -> R,
        ): ServiceProducer<R> {
            return ServiceProducer(params = params.toParams(), creates = creates) { _, p ->
                factory(p)
            }
        }

        private fun List<KType>.toParams(): List<ParameterProvider> =
            mapIndexed { idx, type ->
                ParameterProvider.of(name = "p${idx + 1}", type = type)
            }
    }
}
