package io.peekandpoke.ultra.slumber

import io.peekandpoke.ultra.common.TypedAttributes

/**
 * Serializes ("slumbers") typed Kotlin objects into raw data (Maps, Lists, primitives).
 *
 * Implementations handle specific types. The [Context] carries codec references and path tracking.
 */
interface Slumberer {

    /**
     * Context passed during serialization.
     *
     * Two implementations exist:
     * - [Fast]: Lightweight, no path tracking — used for the first pass.
     * - [Tracking]: Full path tracking — used on error for diagnostics.
     */
    interface Context {

        /** The codec this context belongs to; used for all recursive slumber calls. */
        val codec: Codec

        /** Attributes of the owning [Codec]; how modules such as Vault reach per-codec services. */
        val attributes: TypedAttributes

        /** The current serialization path, e.g. "root.user.address.zip". */
        val path: String

        /** Returns a new context scoped one level deeper in the data structure. */
        fun stepInto(step: String): Context

        /** Recursively slumbers a value, picking the slumberer from the RUNTIME class of [data]. */
        fun slumber(data: Any?): Any? = codec.slumber(data, this)

        /**
         * Throws a [SlumbererException] indicating a non-nullable value was null.
         *
         * The path comes from THIS context, so on a [Fast] context the message carries `<unknown>` —
         * that pass is expected to be retried by [Codec].
         */
        @Throws(SlumbererException::class)
        fun reportNullError(input: Any?): Nothing = throw SlumbererException(
            message = "Value at path '$path' must not be null",
            input = input,
        )

        /**
         * Lightweight context with no path tracking. Used for the first (fast) serialization pass.
         *
         * Immutable and stateless: [stepInto] returns `this`, so one instance is shared by the whole graph.
         */
        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {
            override val path: String = "<unknown>"

            override fun stepInto(step: String): Fast = this
        }

        /**
         * Full-featured context with path tracking. Used on error for diagnostics.
         *
         * [stepInto] appends to an immutable path string, so siblings cannot corrupt each other's path.
         */
        class Tracking internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
            override val path: String,
        ) : Context {

            override fun stepInto(step: String): Tracking = Tracking(
                codec = codec,
                attributes = attributes,
                path = "$path.$step",
            )
        }
    }

    /** Serializes [data] into raw data (Maps, Lists, primitives) using the given [context]. */
    fun slumber(data: Any?, context: Context): Any?
}
