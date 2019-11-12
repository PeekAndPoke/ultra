package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object FloatAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object FloatSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Float? = when (data) {

    is Number -> data.toFloat()

    is String -> data.toFloatOrNull()

    else -> null
}
