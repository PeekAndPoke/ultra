package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object ByteCodec : Awaker, Slumberer {

    override fun awake(data: Any?) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Byte? {

        if (data is Number) {
            return data.toByte()
        }

        if (data is String) {
            return data.toByteOrNull()
        }

        return null
    }
}
