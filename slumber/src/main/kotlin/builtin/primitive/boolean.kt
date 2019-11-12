package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object BooleanAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object BooleanSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Boolean? = when (data) {

    is Boolean -> data

    is Number -> data.toInt() != 0

    is String -> data.toBoolean() || when (data.toIntOrNull()) {
        null -> false
        0 -> false
        else -> true
    }

    else -> null
}
