package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object IntCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Int? {

        if (data is Number) {
            return data.toInt()
        }

        if (data is String) {
            return data.toIntOrNull()
        }

        return null
    }
}
