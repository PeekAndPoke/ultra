package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer

object CollectionSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {

        is Iterable<*> -> map(data, context)

        is Array<*> -> map(data.toList(), context)

        else -> null
    }

    private fun map(data: Iterable<*>, context: Slumberer.Context) = data.mapIndexed { idx, it ->
        context.stepInto(idx.toString()).slumber(it)
    }
}
