package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class MapSlumberer(
    private val keyType: KType,
    private val valueType: KType,
) : Slumberer {

    companion object {
        fun forMap(type: KType): MapSlumberer {
            val keyType = type.arguments[0].type ?: TypeRef.String.type

            val valueType = (type.arguments[1].type ?: TypeRef.Any.type)
                .withNullability(true)

            return MapSlumberer(keyType, valueType)
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Map<*, *>? {

        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) ->
                context.stepInto("$k[KEY]").slumber(keyType, k) to
                        context.stepInto("$k[VAL]").slumber(valueType, v)
            }
            .toMap()
    }
}
