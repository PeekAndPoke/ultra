package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object FloatCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Float? {

        if (data is Number) {
            return data.toFloat()
        }

        if (data is String) {
            return data.toFloatOrNull()
        }

        return null
    }
}
