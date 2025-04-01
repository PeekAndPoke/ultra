package de.peekandpoke.ultra.slumber.builtin.kotlinx

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import kotlinx.serialization.json.JsonNull
import kotlin.reflect.KClass

object KotlinXJsonNullCodec : Awaker, Slumberer {

    fun appliesTo(cls: KClass<*>): Boolean {
        return JsonNull::class.java.isAssignableFrom(cls.java)
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {
        return JsonNull
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        return null
    }
}
