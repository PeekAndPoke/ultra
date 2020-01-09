package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType

class EnumCodec(type: KType) : Awaker, Slumberer {

    @Suppress("UNCHECKED_CAST")
    private val cls = type.classifier as KClass<*>

    private val enumValues = cls.java.enumConstants

    private val lookUp = mutableMapOf<String, Any?>()

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        return lookUp.getOrPut(data) {
            enumValues.firstOrNull { it.toString() == data }
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        return data.toString()
    }
}
