package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.slumber.Slumberer

object MapSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Map<*, *>? {

        if (data !is Map<*, *>) {
            return null
        }

        return data
            .map { (k, v) ->
                context.stepInto("$k[KEY]").slumber(k) to
                        context.stepInto("$k[VAL]").slumber(v)
            }
            .toMap()
    }
}
