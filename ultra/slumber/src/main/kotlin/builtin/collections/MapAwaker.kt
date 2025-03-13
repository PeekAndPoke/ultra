package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class MapAwaker(
    private val keyType: KType,
    private val valueType: KType,
    private val creator: List<Pair<*, *>>.() -> Map<*, *>,
) : Awaker {

    companion object {
        fun forMap(type: KType): MapAwaker {
            val keyType = type.arguments.getOrNull(0)?.type ?: TypeRef.String.type

            val valueType = (type.arguments.getOrNull(1)?.type ?: TypeRef.Any.type).let {
                if (it.classifier != null) {
                    it
                } else {
                    it.withNullability(true)
                }
            }

            return MapAwaker(keyType, valueType) {
                toMap().toMutableMap()
            }
        }
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
