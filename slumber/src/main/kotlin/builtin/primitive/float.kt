package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableFloatAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NonNullFloatAwaker : NonNullAwaker(NullableFloatAwaker)

object NullableFloatSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

object NonNullFloatSlumberer : NonNullSlumberer(NullableFloatSlumberer)

private fun map(data: Any?): Float? = when (data) {

    is Number -> data.toFloat()

    is String -> data.toFloatOrNull()

    else -> null
}
