package de.peekandpoke.ultra.kontainer

import kotlin.reflect.KClass

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
    val producer: ServiceProducer,
    val overwrites: ServiceDefinition?,
    val codeLocation: CodeLocation = CodeLocation.create(),
) {
    class CodeLocation(
        val stack: Array<StackTraceElement>,
    ) {
        companion object {
            fun create(): CodeLocation {

                val originalStack = Throwable().stackTrace

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

                val cleanedStack = when (firstIdx) {
                    -1 -> originalStack

                    else -> originalStack.toList()
                        .subList(fromIndex = firstIdx, originalStack.size)
                        .toTypedArray()
                }

                return CodeLocation(
                    stack = cleanedStack
                )
            }
        }

        fun first(): StackTraceElement? {
            return stack.firstOrNull()
        }
    }

    fun withType(newType: InjectionType) = ServiceDefinition(
        produces = this.produces,
        type = newType,
        producer = this.producer,
        overwrites = this.overwrites,
        codeLocation = this.codeLocation,
    )
}
