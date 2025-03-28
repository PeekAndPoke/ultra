package de.peekandpoke.ultra.slumber

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

object JsonUtil {
    fun JsonObject.unwrap(): Map<String, Any?> {
        return entries.associate { (k, v) ->
            k to v.unwrap()
        }
    }

    fun JsonElement.unwrap(): Any? = when (this) {
        is JsonObject -> unwrap()
        is JsonArray -> map { it.unwrap() }
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> boolean
            longOrNull != null -> long
            doubleOrNull != null -> double
            else -> null
        }
    }

    fun Map<String, Any?>.toJsonObject(): JsonObject {
        return buildJsonObject {
            forEach { (key, value) ->
                put(key.toString(), value.toJsonElement())
            }
        }
    }

    // Helper function to convert any value to JsonElement
    fun Any?.toJsonElement(): JsonElement {
        @Suppress("UNCHECKED_CAST")
        return when (this) {
            null -> JsonNull
            is Map<*, *> -> (this as Map<String, Any?>).toJsonObject()
            is List<*> -> JsonArray(map { it.toJsonElement() })
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            else -> JsonPrimitive(toString())
        }
    }
}
