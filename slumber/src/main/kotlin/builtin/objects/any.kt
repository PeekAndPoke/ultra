package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.slumber.builtin.NonNullAwaker
import de.peekandpoke.ultra.slumber.builtin.NonNullSlumberer

object NullableAnyAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = data
}

object NonNullAnyAwaker : NonNullAwaker(NullableAnyAwaker)

object NullableAnySlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = context.slumber(data)
}

object NonNullAnySlumberer : NonNullSlumberer(NullableAnySlumberer)
