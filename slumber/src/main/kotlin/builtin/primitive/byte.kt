package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableByteAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullByteAwaker : NonNullAwaker(NullableByteAwaker)

object NullableByteSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullByteSlumberer : NonNullSlumberer(NullableByteSlumberer)

private fun map(data: Any?): Byte? = when (data) {

    is Byte -> data

    is Number -> data.toLong().let {
        if (it >= Byte.MIN_VALUE && it <= Byte.MAX_VALUE) {
            data.toByte()
        } else {
            null
        }
    }


    is String -> map(data.toDoubleOrNull())

    else -> null
}
