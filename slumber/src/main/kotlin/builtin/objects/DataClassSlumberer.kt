package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Config
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

class DataClassSlumberer(cls: KClass<*>, config: Config) : Slumberer {

    private val ctor = cls.primaryConstructor!!

    private val properties2Slumberers = ctor.parameters.map { param ->
        cls.declaredMemberProperties
            .mapNotNull { it.javaField }
            .first { it.name == param.name }
            .apply { isAccessible = true } to config.getSlumberer(param.type)
    }

    override fun slumber(data: Any?): Map<String, Any?>? {

        if (data == null) {
            return null
        }

        return properties2Slumberers.map { (prop, slumberer) ->
            prop.name to slumberer.slumber(prop.get(data))
        }.toMap()
    }
}
