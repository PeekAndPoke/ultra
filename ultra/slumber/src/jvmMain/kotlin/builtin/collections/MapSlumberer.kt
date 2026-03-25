package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Slumberer

/** Serializes Maps by recursively slumbering both keys and values. */
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
