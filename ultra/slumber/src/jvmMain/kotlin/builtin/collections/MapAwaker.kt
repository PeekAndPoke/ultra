package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

/**
 * Deserializes raw Maps into typed Kotlin Maps.
 *
 * Both keys and values are recursively awakened using the map's type arguments.
 */
class MapAwaker(
    private val keyType: KType,
    private val valueType: KType,
    private val creator: List<Pair<*, *>>.() -> Map<*, *>,
) : Awaker {

    companion object {
        /** Creates a [MapAwaker] for the given map [type], extracting key and value types from type arguments. */
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
