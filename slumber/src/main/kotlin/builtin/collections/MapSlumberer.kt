package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

class MapSlumberer(
    private val keyType: KType,
    private val valueType: KType
) : Slumberer {

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
