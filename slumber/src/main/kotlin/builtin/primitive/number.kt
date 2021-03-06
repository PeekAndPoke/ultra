package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object NumberAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object NumberSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Number? = when (data) {

    is Number -> data

    is String -> data.toDoubleOrNull()

    else -> null
}
