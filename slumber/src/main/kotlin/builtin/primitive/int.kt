package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object IntAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object IntSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Int? = when (data) {

    is Int -> data

    is Short -> data.toInt()

    is Byte -> data.toInt()

    is Number -> data.toLong().let {
        if (it >= Int.MIN_VALUE && it <= Int.MAX_VALUE) {
            data.toInt()
        } else {
            null
        }
    }

    is String -> map(data.toDoubleOrNull())

    else -> null
}
