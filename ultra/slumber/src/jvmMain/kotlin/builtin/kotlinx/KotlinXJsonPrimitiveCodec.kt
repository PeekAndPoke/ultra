package io.peekandpoke.ultra.slumber.builtin.kotlinx

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.JsonUtil.unwrap
import io.peekandpoke.ultra.slumber.Slumberer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClass

/** Codec for KotlinX [JsonPrimitive]. Awakes scalar values into a [JsonPrimitive]; slumbers back to a scalar. */
object KotlinXJsonPrimitiveCodec : Awaker, Slumberer {

    /** Returns true if [cls] is [JsonPrimitive]. */
    fun appliesTo(cls: KClass<*>): Boolean {
        return JsonPrimitive::class.java.isAssignableFrom(cls.java)
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        return when (data) {
            is JsonPrimitive -> data
            is Boolean -> JsonPrimitive(data)
            is Number -> JsonPrimitive(data)
            is String -> JsonPrimitive(data)
            else -> null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        if (data !is JsonPrimitive) {
            return null
        }

        return (data as JsonElement).unwrap()
    }
}
