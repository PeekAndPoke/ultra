package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType

/**
 * Deserializes arrays and iterables into typed List or Set collections.
 *
 * Each element is recursively awakened using the collection's type argument.
 */
class CollectionAwaker(
    private val innerType: KType,
    private val creator: List<*>.() -> Any
) : Awaker {

    companion object {

        /** Creates a [CollectionAwaker] that produces a [MutableList]. */
        fun forList(type: KType) = CollectionAwaker(type.arguments[0].type!!) {
            toMutableList()
        }

        /** Creates a [CollectionAwaker] that produces a [MutableSet]. */
        fun forSet(type: KType) = CollectionAwaker(type.arguments[0].type!!) {
            toMutableSet()
        }
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? = when (data) {

        is Array<*> -> awakeInternal(data.toList(), context)

        is Iterable<*> -> awakeInternal(data.toList(), context)

        else -> null
    }

    private fun awakeInternal(data: List<*>, context: Awaker.Context): Any {

        return data
            .mapIndexed { idx, item ->
                context.stepInto(idx.toString()).awake(innerType, item)
            }
            .creator()
    }
}
