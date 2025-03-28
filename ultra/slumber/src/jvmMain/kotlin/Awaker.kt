package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KClass
import kotlin.reflect.KType

interface Awaker {

    interface Context {

        val codec: Codec
        val rootType: KType?
        val attributes: TypedAttributes
        val path: String
        val logs: List<String>

        fun stepInto(step: String): Context

        fun <T : Any> awake(type: KClass<T>, data: Any?): T? = codec.awake(type, data, this)

        fun awake(type: KType, data: Any?): Any? = codec.awake(type, data, this)

        fun log(provider: () -> String) {
            // default does nothing
        }

        @Throws(AwakerException::class)
        fun reportNullError(input: Any?, context: Context): Nothing = throw AwakerException(
            message = listOf("Value at path '$path' must not be null")
                .plus("Root Type: $rootType")
                .plus("Input type: ${input?.let { it::class.qualifiedName }}")
                .plus("Input value: ${input?.toString()?.take(8192)}")
                .plus(logs)
                .joinToString("\n"),
            logs = logs,
            rootType = rootType,
            input = input,
        )

        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {

            override val path: String = "<unknown>"

            override val rootType: KType? = null

            override val logs: List<String> = emptyList()

            override fun stepInto(step: String): Fast = this
        }

        class Tracking internal constructor(
            override val codec: Codec,
            override val rootType: KType,
            override val attributes: TypedAttributes,
            override val path: String,
            override val logs: MutableList<String>,
        ) : Context {

            override fun stepInto(step: String): Tracking = Tracking(
                codec = codec,
                rootType = rootType,
                attributes = attributes,
                path = "$path.$step",
                logs = logs,
            )

            override fun log(provider: () -> String) {
                logs.add("  - '$path': ${provider()}")
            }
        }
    }

    fun awake(data: Any?, context: Context): Any?
}
