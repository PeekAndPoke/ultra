package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object CharAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object CharSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Char? = when {

    data is Char -> data

    data is String && data.length > 0 -> data[0]

    else -> null
}
