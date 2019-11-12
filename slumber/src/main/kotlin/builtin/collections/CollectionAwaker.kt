package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import kotlin.reflect.KType

class CollectionAwaker(
    private val innerType: KType,
    private val creator: List<*>.() -> Any
) : Awaker {

    companion object {

        fun forList(type: KType) = CollectionAwaker(type.arguments[0].type!!) { toList() }.let {
            if (type.isMarkedNullable) it else NonNullAwaker(it)
        }

        fun forMutableList(innerType: KType) =
            CollectionAwaker(innerType) { toMutableList() }

        fun forSet(innerType: KType) =
            CollectionAwaker(innerType) { toSet() }

        fun forMutableSet(innerType: KType) =
            CollectionAwaker(innerType) { toMutableSet() }
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? = when (data) {

        is Array<*> -> awakeInternal(data.toList(), context)

        is Iterable<*> -> awakeInternal(data, context)

        else -> null
    }

    private fun awakeInternal(data: Iterable<*>, context: Awaker.Context): Any? {

        return data
            .mapIndexed { idx, item -> context.stepInto(idx.toString()).awake(innerType, item) }
            .creator()
    }
}
