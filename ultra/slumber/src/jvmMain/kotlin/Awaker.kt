package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Deserializes ("awakens") raw data into typed Kotlin objects.
 *
 * Implementations handle specific types (primitives, data classes, collections, etc.).
 * The [Context] carries codec references, path tracking, and diagnostic logging.
 */
interface Awaker {

    /**
     * Context passed during deserialization, providing access to the codec, path tracking, and logging.
     *
     * Two implementations exist:
     * - [Fast]: Lightweight, no path tracking — used for the first pass.
     * - [Tracking]: Full path tracking and logging — used on error for diagnostics.
     */
    interface Context {

        val codec: Codec
        val rootType: KType?
        val attributes: TypedAttributes

        /** The current deserialization path, e.g. "root.user.address.zip". */
        val path: String
        val logs: List<String>

        /** Returns a new context scoped one level deeper in the data structure. */
        fun stepInto(step: String): Context

        /** Recursively awakens a value using the codec. */
        fun <T : Any> awake(type: KClass<T>, data: Any?): T? = codec.awake(type, data, this)

        /** Recursively awakens a value using the codec. */
        fun awake(type: KType, data: Any?): Any? = codec.awake(type, data, this)

        /** Logs a diagnostic message. Only [Tracking] actually records these. */
        fun log(provider: () -> String) {
            // default does nothing
        }

        /** Throws an [AwakerException] with full path and diagnostic information. */
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

        /** Lightweight context with no path tracking. Used for the first (fast) deserialization pass. */
        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {

            override val path: String = "<unknown>"

            override val rootType: KType? = null

            override val logs: List<String> = emptyList()

            override fun stepInto(step: String): Fast = this
        }

        /** Full-featured context with path tracking and log collection. Used on error for diagnostics. */
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

    /** Deserializes [data] into a typed object using the given [context]. Returns null if the data cannot be handled. */
    fun awake(data: Any?, context: Context): Any?
}
