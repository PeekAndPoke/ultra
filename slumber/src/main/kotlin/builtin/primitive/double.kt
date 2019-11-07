package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableDoubleAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullDoubleAwaker : NonNullAwaker(NullableDoubleAwaker)

object NullableDoubleSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullDoubleSlumberer : NonNullSlumberer(NullableDoubleSlumberer)

private fun map(data: Any?): Double? = when (data) {

    is Number -> data.toDouble()

    is String -> data.toDoubleOrNull()

    else -> null
}
