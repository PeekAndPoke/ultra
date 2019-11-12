package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object DoubleAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object DoubleSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Double? = when (data) {

    is Number -> data.toDouble()

    is String -> data.toDoubleOrNull()

    else -> null
}
