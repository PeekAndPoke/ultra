package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Shared
import kotlin.reflect.KType

class CollectionAwaker(
    private val innerType: KType,
    shared: Shared,
    private val creator: List<*>.() -> Any
) : Awaker {

    companion object {

        fun forList(innerType: KType, shared: Shared) =
            CollectionAwaker(innerType, shared) { toList() }

        fun forMutableList(innerType: KType, shared: Shared) =
            CollectionAwaker(innerType, shared) { toMutableList() }

        fun forSet(innerType: KType, shared: Shared) =
            CollectionAwaker(innerType, shared) { toSet() }

        fun forMutableSet(innerType: KType, shared: Shared) =
            CollectionAwaker(innerType, shared) { toMutableSet() }
    }

    private val itemAwaker = shared.getAwaker(innerType)

    override fun awake(data: Any?): Any? {

        if (data is Array<*>) {
            return awakeInternal(data.toList())
        }

        if (data is Iterable<*>) {
            return awakeInternal(data)
        }

        return null
    }

    private fun awakeInternal(data: Iterable<*>): Any? {

        val intermediate = data.map { itemAwaker.awake(it) }

        if (innerType.isMarkedNullable) {
            return intermediate.creator()
        }

        return intermediate.filterNotNull().creator()
    }
}
