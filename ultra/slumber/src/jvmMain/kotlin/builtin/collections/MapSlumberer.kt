package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Slumberer

/** Serializes Maps by recursively slumbering both keys and values, each by its runtime type. */
object MapSlumberer : Slumberer {

    /** Input that is not a `Map` slumbers to `null`. */
    override fun slumber(data: Any?, context: Slumberer.Context): Map<*, *>? {

        if (data !is Map<*, *>) {
            return null
        }

        // TODO(scan): keys are slumbered too, so (a) two keys whose slumbered forms are equal collapse
        //  silently in toMap() — last one wins — and (b) a structured key (data class, date) becomes a
        //  Map used as a key, which no JSON writer can represent as an object key.
        return data
            .map { (k, v) ->
                // TODO(scan): the RAW key goes into the path unescaped and unbounded; see MapAwaker.
                context.stepInto("$k[KEY]").slumber(k) to
                        context.stepInto("$k[VAL]").slumber(v)
            }
            .toMap()
    }
}
