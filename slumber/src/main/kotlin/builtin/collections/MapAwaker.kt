package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType

class MapAwaker(
    private val keyType: KType,
    private val valueType: KType,
    private val creator: List<Pair<*, *>>.() -> Map<*, *>
) : Awaker {

    companion object {
        fun forMap(type: KType) =
            MapAwaker(type.arguments[0].type!!, type.arguments[1].type!!) { toMap() }

        fun forMutableMap(type: KType) =
            MapAwaker(type.arguments[0].type!!, type.arguments[1].type!!) { toMap().toMutableMap() }
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) ->
                context.stepInto("$k[KEY]").awake(keyType, k) to
                        context.stepInto("$k[VAL]").awake(valueType, v)
            }
            .creator()
    }
}
