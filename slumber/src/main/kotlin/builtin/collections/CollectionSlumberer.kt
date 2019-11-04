package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Config
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

class CollectionSlumberer(
    innerType: KType,
    config: Config
) : Slumberer {

    private val itemSlumberer = config.getSlumberer(innerType)

    override fun slumber(data: Any?): Any? {

        if (data !is Iterable<*>) {
            return null
        }

        return data.map { itemSlumberer.slumber(it) }
    }
}
