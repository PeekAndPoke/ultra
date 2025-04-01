package de.peekandpoke.ultra.slumber.builtin.kotlinx

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import de.peekandpoke.ultra.slumber.JsonUtil.unwrap
import de.peekandpoke.ultra.slumber.Slumberer
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

object KotlinXJsonCodec : Awaker, Slumberer {

    fun appliesTo(cls: KClass<*>): Boolean {
        return JsonElement::class.java.isAssignableFrom(cls.java)
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        return data.toJsonElement()
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        if (data !is JsonElement) {
            return null
        }

        return data.unwrap()
    }
}
