package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object StringCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): String? {

        if (data is String) {
            return data
        }

        if (data != null && data::class.java.isPrimitive) {
            return data.toString()
        }

        return null
    }
}
