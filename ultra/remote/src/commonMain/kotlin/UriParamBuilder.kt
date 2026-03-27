package io.peekandpoke.ultra.remote

import io.peekandpoke.ultra.common.encodeUriComponent

/**
 * DSL builder for constructing URI parameter maps used by [buildUri].
 *
 * Values are stored as typed [Value] wrappers so that encoding can be
 * deferred and applied uniformly when the final URI string is assembled.
 *
 * Usage:
 * ```kotlin
 * val params = UriParamBuilder.uriParams {
 *     set("page", 1)
 *     set("q", "hello world")
 *     setRaw("token", "abc123")
 * }
 * ```
 */
class UriParamBuilder private constructor() {
    companion object {
        /**
         * Converts a plain `Map<String, String?>` into a typed parameter map
         * by wrapping each entry in a [StrValue].
         */
        fun of(map: Map<String, String?>): Map<String, Value> = uriParams {
            map.forEach { (k, v) -> set(k, v) }
        }

        /**
         * Creates a typed parameter map using the [UriParamBuilder] DSL.
         */
        fun uriParams(block: UriParamBuilder.() -> Unit): Map<String, Value> = UriParamBuilder().apply(block).build()
    }

    /**
     * A URI parameter value that may be `null` and exposes its
     * URI-encoded form via [encoded].
     */
    interface Value {
        /** The URI-encoded representation, or `null` when the value is absent. */
        val encoded: String?
    }

    /** A [Value] wrapping a nullable [String], URI-encoded on construction. */
    data class StrValue(val str: String?) : Value {
        override val encoded = str?.encodeUriComponent()
    }

    /** A [Value] wrapping a nullable [Number], converted to string and URI-encoded. */
    data class NumberValue(val num: Number?) : Value {
        override val encoded = num?.toString()?.encodeUriComponent()
    }

    /** A [Value] wrapping a nullable [Boolean], converted to string and URI-encoded. */
    data class BooleanValue(val bool: Boolean?) : Value {
        override val encoded = bool?.toString()?.encodeUriComponent()
    }

    /** A [Value] whose string is used **as-is** without any encoding. */
    data class RawValue(val raw: String?) : Value {
        override val encoded = raw
    }

    private val values = mutableMapOf<String, Value>()

    internal fun build(): Map<String, Value> = values.toMap()

    /** Sets a parameter by its typed [Value] wrapper. */
    fun set(name: String, value: Value) {
        values[name] = value
    }

    /** Sets a string parameter (will be URI-encoded). */
    fun set(name: String, value: String?) = set(name, StrValue(value))

    /** Sets a numeric parameter (will be URI-encoded). */
    fun set(name: String, value: Number?) = set(name, NumberValue(value))

    /** Sets a boolean parameter (will be URI-encoded). */
    fun set(name: String, value: Boolean?) = set(name, BooleanValue(value))

    /** Sets a raw parameter whose value will **not** be URI-encoded. */
    fun setRaw(name: String, value: String?) = set(name, RawValue(value))
}
