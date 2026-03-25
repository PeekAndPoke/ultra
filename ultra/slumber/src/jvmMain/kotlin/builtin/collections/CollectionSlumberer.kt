package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Slumberer

/** Serializes Iterables and Arrays into a list of recursively slumbered elements. */
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
