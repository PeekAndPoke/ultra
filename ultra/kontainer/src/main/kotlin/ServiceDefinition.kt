package io.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

/**
 * Defines a service
 *
 * [serviceCls] is the base class as which this service is registered.
 *
 * [type] and [kind] are what the service is declared as; the kind is [ServiceKind.Static] or
 * [ServiceKind.Dynamic], as [ServiceKind.SemiDynamic] is only ever derived by the [KontainerBlueprint].
 *
 * [ServiceProducer] is used to create an instance of the service.
 */
class ServiceDefinition internal constructor(
    val serviceCls: KClass<*>,
    val type: ServiceType,
    val kind: ServiceKind,
    val producer: ServiceProducer<*>,
    val overwrites: ServiceDefinition?,
    val codeLocation: CodeLocation = CodeLocation.create(),
) {
    init {
        require(kind != ServiceKind.SemiDynamic) { "SemiDynamic cannot be declared, it is derived by the blueprint" }
    }

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
                "io.peekandpoke.ultra.kontainer.IndexKt",
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

    /** Returns a copy of this definition declared as [type] and [kind] */
    fun withScope(type: ServiceType, kind: ServiceKind) = ServiceDefinition(
        serviceCls = this.serviceCls,
        type = type,
        kind = kind,
        producer = this.producer,
        overwrites = this.overwrites,
        codeLocation = this.codeLocation,
    )
}
