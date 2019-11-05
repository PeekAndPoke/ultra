package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer

object MapSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<*, *>? {
        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) -> context.slumber(k) to context.slumber(v) }
            .toMap()
    }
}
