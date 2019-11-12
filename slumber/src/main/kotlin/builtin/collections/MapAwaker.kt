package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType

class MapAwaker(
    private val keyType: KType,
    private val valueType: KType
) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) -> context.awake(keyType, k) to context.awake(valueType, v) }
            .toMap()
    }
}
