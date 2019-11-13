package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

class CollectionSlumberer(private val innerType: KType) : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {

        is Iterable<*> -> map(data, context)

        is Array<*> -> map(data.toList(), context)

        else -> null
    }

    private fun map(data: Iterable<*>, context: Slumberer.Context) = data.mapIndexed { idx, it ->
        context.stepInto(idx.toString()).slumber(innerType, it)
    }
}
