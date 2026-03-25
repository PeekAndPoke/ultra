package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Defines a service
 *
 * [serviceCls] is the base class as which this service is registered.
 *
 * [injectionType] is how the service is to be injected.
 *
 * [ServiceProducer] is used to create an instance of the service.
 */
class ServiceDefinition internal constructor(
    val serviceCls: KClass<*>,
    val injectionType: InjectionType,
    val producer: ServiceProducer<*>,
    val overwrites: ServiceDefinition?,
    val codeLocation: CodeLocation = CodeLocation.create(),
) {
    /** Captures the code location where a service was defined, for debugging */
    class CodeLocation private constructor(
        private val throwable: Throwable,
    ) {
        companion object {
            /** Creates a [CodeLocation] capturing the current stack trace */
            fun create(): CodeLocation {
                return CodeLocation(
                    throwable = Throwable()
                )
            }
        }

        /** The filtered stack trace, excluding Kontainer internals */
        val stack: Array<StackTraceElement> by lazy {
            val originalStack = throwable.stackTrace

            val excludes = listOf(
                "de.peekandpoke.ultra.kontainer.IndexKt",
            ).plus(
                listOf(
                    CodeLocation::class,
                    Companion::class,
                    KontainerBuilder::class,
                    KontainerBuilder.ServiceBuilder::class,
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

        /** The filtered stack trace as a formatted string */
        val stackPrinted: String by lazy {
            val newThrowable = Throwable()
            newThrowable.stackTrace = stack

            newThrowable.stackTraceToString()
        }

        /** Returns the first (most relevant) stack trace element, or null */
        fun first(): StackTraceElement? {
            return stack.firstOrNull()
        }
    }

    /** Returns a copy of this definition with a different [newType] */
    fun withType(newType: InjectionType) = ServiceDefinition(
        serviceCls = this.serviceCls,
        injectionType = newType,
        producer = this.producer,
        overwrites = this.overwrites,
        codeLocation = this.codeLocation,
    )
}
