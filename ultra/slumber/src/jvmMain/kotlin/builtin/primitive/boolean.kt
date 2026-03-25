package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Boolean] values. */
object BooleanAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Boolean] values. */
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
