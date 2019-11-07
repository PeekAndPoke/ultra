package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableStringAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullStringAwaker : NonNullAwaker(NullableStringAwaker)

object NullableStringSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullStringSlumberer : NonNullSlumberer(NullableStringSlumberer)

private fun map(data: Any?): String? = when (data) {

    is String -> data

    is Number -> data.toString()

    is Char -> data.toString()

    is Boolean -> data.toString()

    else -> null
}
