package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object BooleanCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Boolean? {

        if (data is Boolean) {
            return data
        }

        if (data is Number) {
            return data.toInt() == 0
        }

        if (data is String) {
            return data.toBoolean()
        }

        return null
    }
}
