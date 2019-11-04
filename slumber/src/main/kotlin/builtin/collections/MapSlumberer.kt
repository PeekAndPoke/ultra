package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Config
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

class MapSlumberer(keyType: KType, valueType: KType, config: Config) : Slumberer {

    private val keySlumberer = config.getSlumberer(keyType)
    private val valueSlumberer = config.getSlumberer(valueType)

    override fun slumber(data: Any?): Map<*, *>? {
        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) -> keySlumberer.slumber(k) to valueSlumberer.slumber(v) }
            .toMap()
    }
}
