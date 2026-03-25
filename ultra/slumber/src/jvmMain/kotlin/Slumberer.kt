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

        val codec: Codec
        val attributes: TypedAttributes

        /** The current serialization path, e.g. "root.user.address.zip". */
        val path: String

        /** Returns a new context scoped one level deeper in the data structure. */
        fun stepInto(step: String): Context

        /** Recursively slumbers a value using the codec. */
        fun slumber(data: Any?): Any? = codec.slumber(data, this)

        /** Throws a [SlumbererException] indicating a non-nullable value was null. */
        @Throws(SlumbererException::class)
        fun reportNullError(input: Any?): Nothing = throw SlumbererException(
            message = "Value at path '$path' must not be null",
            input = input,
        )

        /** Lightweight context with no path tracking. Used for the first (fast) serialization pass. */
        class Fast internal constructor(
            override val codec: Codec,
            override val attributes: TypedAttributes,
        ) : Context {
            override val path: String = "<unknown>"

            override fun stepInto(step: String): Fast = this
        }

        /** Full-featured context with path tracking. Used on error for diagnostics. */
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
