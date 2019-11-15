package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType

class CollectionAwaker(
    private val innerType: KType,
    private val creator: List<*>.() -> Any
) : Awaker {

    companion object {

        fun forList(type: KType) = CollectionAwaker(type.arguments[0].type!!) { toList() }

        fun forMutableList(type: KType) = CollectionAwaker(type.arguments[0].type!!) { toMutableList() }

        fun forSet(type: KType) = CollectionAwaker(type.arguments[0].type!!) { toSet() }

        fun forMutableSet(type: KType) = CollectionAwaker(type.arguments[0].type!!) { toMutableSet() }
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? = when (data) {

        is Array<*> -> awakeInternal(data.toList(), context)

        is Iterable<*> -> awakeInternal(data, context)

        else -> null
    }

    private fun awakeInternal(data: Iterable<*>, context: Awaker.Context): Any? {

        return data
            .mapIndexed { idx, item ->
                context.stepInto(idx.toString()).awake(innerType, item)
            }
            .creator()
    }
}
