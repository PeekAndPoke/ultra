package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object BooleanCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context) = map(data)

    override fun slumber(data: Any?) = map(data)

    private fun map(data: Any?): Boolean? = when (data) {

        is Boolean -> data

        is Number -> data.toInt() != 0

        is String -> data.toBoolean()

        else -> null
    }
}
