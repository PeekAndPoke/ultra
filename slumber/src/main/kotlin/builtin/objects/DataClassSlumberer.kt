package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

class DataClassSlumberer(cls: KClass<*>) : Slumberer {

    private val ctor = cls.primaryConstructor!!

    private val properties = ctor.parameters.map { param ->
        cls.declaredMemberProperties
            .mapNotNull { it.javaField }
            .first { it.name == param.name }
            .apply { isAccessible = true }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? = when {

        data != null -> properties.map { prop -> prop.name to context.slumber(prop.get(data)) }.toMap()

        else -> null
    }
}
