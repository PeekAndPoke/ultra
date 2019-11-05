package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer

object CollectionSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data !is Iterable<*>) {
            return null
        }

        return data.map { context.slumber(it) }
    }
}
