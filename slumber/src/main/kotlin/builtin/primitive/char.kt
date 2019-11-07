package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableCharAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullCharAwaker : NonNullAwaker(NullableCharAwaker)

object NullableCharSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullCharSlumberer : NonNullSlumberer(NullableCharSlumberer)

private fun map(data: Any?): Char? = when {

    data is Char -> data

    data is String && data.length > 0 -> data[0]

    else -> null
}
