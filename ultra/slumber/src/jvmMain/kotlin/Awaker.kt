package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.common.TypedAttributes
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

        /** The codec this context belongs to; used for all recursive awake calls. */
        val codec: Codec

        /** The type the whole operation started from, or null when it is not tracked ([Fast]). */
        val rootType: KType?

        /** Attributes of the owning [Codec]; how modules such as Vault reach per-codec services. */
        val attributes: TypedAttributes

        /** The current deserialization path, e.g. "root.user.address.zip". */
        val path: String

        /** Diagnostics collected so far. Always empty on [Fast]. */
        val logs: List<String>

        /** Returns a new context scoped one level deeper in the data structure. */
        fun stepInto(step: String): Context

        /** Recursively awakens a value; the type arguments of [type] are approximated. */
        fun <T : Any> awake(type: KClass<T>, data: Any?): T? = codec.awake(type, data, this)

        /** Recursively awakens a value using the codec. */
        fun awake(type: KType, data: Any?): Any? = codec.awake(type, data, this)

        /** Logs a diagnostic message. Only [Tracking] actually records these. */
        fun log(provider: () -> String) {
            // default does nothing
        }

        /**
         * Throws an [AwakerException] describing a null where a non-null value was required.
         *
         * Path, root type and logs come from THIS context, so on a [Fast] context the message carries
         * `<unknown>`, a null root type and no logs — that pass is expected to be retried by [Codec].
         */
        // TODO(scan): the `context` parameter is never read; the body uses the receiver throughout.
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

        /**
         * Lightweight context with no path tracking. Used for the first (fast) deserialization pass.
         *
         * Immutable and stateless: [stepInto] returns `this`, so one instance is shared by the whole graph.
         */
        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {

            override val path: String = "<unknown>"

            override val rootType: KType? = null

            override val logs: List<String> = emptyList()

            override fun stepInto(step: String): Fast = this
        }

        /**
         * Full-featured context with path tracking and log collection. Used on error for diagnostics.
         *
         * [stepInto] appends to an immutable path string, so siblings cannot corrupt each other's path.
         * The [logs] buffer, in contrast, is SHARED down the whole tree: an error report also contains
         * diagnostics written while awaking siblings that ultimately succeeded.
         */
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

    /** Deserializes [data] using [context]. Returns null when the data cannot be handled. */
    fun awake(data: Any?, context: Context): Any?
}
