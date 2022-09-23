package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class CollectionSlumberer(private val innerType: KType) : Slumberer {

    companion object {
        fun forIterable(type: KType): CollectionSlumberer {
            val valueType = (type.arguments.getOrNull(0)?.type ?: TypeRef.Any.type)
                .withNullability(true)

            return CollectionSlumberer(valueType)
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {

        is Iterable<*> -> map(data, context)

        is Array<*> -> map(data.toList(), context)

        else -> null
    }

    private fun map(data: Iterable<*>, context: Slumberer.Context) = data.mapIndexed { idx, it ->
        context.stepInto(idx.toString()).slumber(innerType, it)
    }
}
