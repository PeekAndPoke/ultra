package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableNumberAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullNumberAwaker : NonNullAwaker(NullableNumberAwaker)

object NullableNumberSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullNumberSlumberer : NonNullSlumberer(NullableNumberSlumberer)

private fun map(data: Any?): Number? = when (data) {

    is Number -> data

    is String -> data.toDoubleOrNull()

    else -> null
}
