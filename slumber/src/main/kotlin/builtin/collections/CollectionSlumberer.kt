package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Shared
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

class CollectionSlumberer(
    innerType: KType,
    shared: Shared
) : Slumberer {

    private val itemSlumberer = shared.getSlumberer(innerType)

    override fun slumber(data: Any?): Any? {

        if (data !is Iterable<*>) {
            return null
        }

        return data.map { itemSlumberer.slumber(it) }
    }
}
