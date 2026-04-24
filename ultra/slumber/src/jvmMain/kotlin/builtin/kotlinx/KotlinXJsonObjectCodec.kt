package io.peekandpoke.ultra.slumber.builtin.kotlinx

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonObject
import io.peekandpoke.ultra.slumber.JsonUtil.unwrap
import io.peekandpoke.ultra.slumber.Slumberer
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

/** Codec for KotlinX [JsonObject]. Awakes a [Map] into a [JsonObject]; slumbers back to a [Map]. */
object KotlinXJsonObjectCodec : Awaker, Slumberer {

    /** Returns true if [cls] is [JsonObject]. */
    fun appliesTo(cls: KClass<*>): Boolean {
        return JsonObject::class.java.isAssignableFrom(cls.java)
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        @Suppress("UNCHECKED_CAST")
        return when (data) {
            is JsonObject -> data
            is Map<*, *> -> (data as Map<String, Any?>).toJsonObject()
            else -> null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        if (data !is JsonObject) {
            return null
        }

        return data.unwrap()
    }
}
