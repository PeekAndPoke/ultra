package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableBooleanAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullBooleanAwaker : NonNullAwaker(NullableBooleanAwaker)

object NullableBooleanSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullBooleanSlumberer : NonNullSlumberer(NullableBooleanSlumberer)

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
