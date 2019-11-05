package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object ByteCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context) = map(data)

    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)

    private fun map(data: Any?): Byte? = when (data) {

        is Number -> data.toByte()

        is String -> data.toByteOrNull()

        else -> null
    }
}
