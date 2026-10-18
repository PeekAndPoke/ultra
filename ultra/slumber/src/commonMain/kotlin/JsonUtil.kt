package io.peekandpoke.ultra.slumber

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

/** Utilities for converting between kotlinx.serialization [JsonElement] trees and plain Kotlin types. */
object JsonUtil {
    /** Unwraps a [JsonObject] into a plain [Map] of string keys to native Kotlin values. */
    fun JsonObject.unwrap(): Map<String, Any?> {
        return entries.associate { (k, v) ->
            k to v.unwrap()
        }
    }

    /** Recursively unwraps a [JsonElement] into its plain Kotlin equivalent (null, Map, List, String, Boolean, Long, or Double). */
    fun JsonElement.unwrap(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> unwrap()
        is JsonArray -> map { it.unwrap() }
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> boolean
            longOrNull != null -> long
            // TODO(scan): numbers beyond Long.MAX_VALUE (e.g. Json.parseToJsonElement("99999999999999999999"))
            //  fall through to here and silently lose precision as a Double instead of erroring.
            doubleOrNull != null -> double
            // TODO(scan): content that is not string/boolean/long/double-parseable (reachable via the
            //  experimental JsonUnquotedLiteral API) is silently dropped to null instead of erroring.
            else -> null
        }
    }

    /** Converts a plain [Map] of string keys to native values into a [JsonObject]. */
    fun Map<String, Any?>.toJsonObject(): JsonObject {
        return buildJsonObject {
            forEach { (key, value) ->
                put(key, value.toJsonElement())
            }
        }
    }

    /**
     * Converts any nullable value to its [JsonElement] representation.
     *
     * An unrecognised type falls back to its `toString()` as a [JsonPrimitive] string.
     */
    fun Any?.toJsonElement(): JsonElement {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            null -> JsonNull
            is JsonElement -> this
            // TODO(scan): unchecked cast — a Map with non-String keys throws a raw ClassCastException here
            //  instead of the library's structured Awaker/SlumbererException.
            is Map<*, *> -> (this as Map<String, Any?>).toJsonObject()
            is List<*> -> JsonArray(map { it.toJsonElement() })
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            else -> JsonPrimitive(toString())
        }
    }
}
