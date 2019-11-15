package de.peekandpoke.ultra.slumber.builtin.polymorphism

import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass

class PolymorphicSlumberer(
    private val discriminator: String,
    private val map: Map<KClass<*>, String>,
    private val default: KClass<*>?
) : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        // get the type identifier
        val type = map[data::class]

        return when (val result = context.slumber(data)) {
            is Map<*, *> -> result.plus(discriminator to type)
            else -> result
        }
    }
}
