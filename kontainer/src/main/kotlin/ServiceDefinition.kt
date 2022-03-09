package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Defines a service
 *
 * Specifies which class the definitions [produces].
 * Specifies the [injectionType] of the service.
 * The [ServiceProducer] is used to create an instance of the service
 */
class ServiceDefinition internal constructor(
    val produces: KClass<*>,
    val injectionType: InjectionType,
    val producer: ServiceProducer,
    val overwrites: ServiceDefinition?,
    val codeLocation: CodeLocation = CodeLocation.create(),
) {
    class CodeLocation private constructor(
        private val throwable: Throwable,
    ) {
        companion object {
            fun create(): CodeLocation {
                return CodeLocation(
                    throwable = Throwable()
                )
            }
        }

        val stack: Array<StackTraceElement> by lazy {
            val originalStack = throwable.stackTrace

            val excludes = listOf(
                "de.peekandpoke.ultra.kontainer.IndexKt",
            ).plus(
                listOf(
                    CodeLocation::class,
                    Companion::class,
                    KontainerBuilder::class,
                    ServiceDefinition::class,
                ).map { it.java.name }
            )

            val firstIdx = originalStack.indexOfFirst {
                it.className !in excludes
            }

            when (firstIdx) {
                -1 -> originalStack

                else -> originalStack.toList()
                    .subList(fromIndex = firstIdx, originalStack.size)
                    .toTypedArray()
            }
        }

        val stackPrinted: String by lazy {
            val newThrowable = Throwable()
            newThrowable.stackTrace = stack

            newThrowable.stackTraceToString()
        }

        fun first(): StackTraceElement? {
            return stack.firstOrNull()
        }
    }

    fun withType(newType: InjectionType) = ServiceDefinition(
        produces = this.produces,
        injectionType = newType,
        producer = this.producer,
        overwrites = this.overwrites,
        codeLocation = this.codeLocation,
    )
}
