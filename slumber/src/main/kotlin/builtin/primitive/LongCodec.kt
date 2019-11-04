package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object LongCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Long? {

        if (data is Number) {
            return data.toLong()
        }

        if (data is String) {
            return data.toLongOrNull()
        }

        return null
    }
}
