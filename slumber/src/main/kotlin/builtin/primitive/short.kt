package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableShortAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullShortAwaker : NonNullAwaker(NullableShortAwaker)

object NullableShortSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullShortSlumberer : NonNullSlumberer(NullableShortSlumberer)

private fun map(data: Any?): Short? = when (data) {

    is Short -> data

    is Byte -> data.toShort()

    is Number -> data.toLong().let {
        if (it >= Short.MIN_VALUE && it <= Short.MAX_VALUE) {
            data.toShort()
        } else {
            null
        }
    }

    is String -> map(data.toDoubleOrNull())

    else -> null
}
