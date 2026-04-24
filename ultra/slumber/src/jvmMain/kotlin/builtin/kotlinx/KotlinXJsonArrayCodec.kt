package io.peekandpoke.ultra.slumber.builtin.kotlinx

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.JsonUtil.unwrap
import io.peekandpoke.ultra.slumber.Slumberer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

/** Codec for KotlinX [JsonArray]. Awakes a [List] into a [JsonArray]; slumbers back to a [List]. */
object KotlinXJsonArrayCodec : Awaker, Slumberer {

    /** Returns true if [cls] is [JsonArray]. */
    fun appliesTo(cls: KClass<*>): Boolean {
        return JsonArray::class.java.isAssignableFrom(cls.java)
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        return when (data) {
            is JsonArray -> data
            is List<*> -> JsonArray(data.map { it.toJsonElement() })
            else -> null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        if (data !is JsonArray) {
            return null
        }

        return (data as JsonElement).unwrap()
    }
}
