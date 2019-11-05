package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object IntCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context) = map(data)

    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)

    private fun map(data: Any?): Int? = when (data) {

        is Number -> data.toInt()

        is String -> data.toIntOrNull()

        else -> null
    }
}
