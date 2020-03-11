package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KClass

class PolymorphicAwaker(
    private val discriminator: String,
    private val map: Map<String, KClass<*>>,
    private val default: KClass<*>?
) : Awaker {

    companion object {
        private val IdentifierType = TypeRef.String.nullable.type
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        // get the type
        val type = context.awake(IdentifierType, data[discriminator])

        // get the target class or the default if present
        val target = type?.let { identifier -> map[identifier] } ?: default

        // awake the target class
        return target?.let { cls -> context.awake(cls, data) }
    }
}
