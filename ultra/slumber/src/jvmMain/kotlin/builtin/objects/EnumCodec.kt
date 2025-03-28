package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType

class EnumCodec(type: KType) : Awaker, Slumberer {

    @Suppress("UNCHECKED_CAST")
    private val cls = type.classifier as KClass<*>

    /** Lookup for enum values by string */
    private val lookUp = cls.java.enumConstants
        .mapNotNull { it as? Enum<*> }
        .map { it.name to it }
        .toMap()

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        return lookUp[data]
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        return when (data) {
            is Enum<*> -> data.name
            else -> null
        }
    }
}
