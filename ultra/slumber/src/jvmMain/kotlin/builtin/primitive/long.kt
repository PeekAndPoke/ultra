package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer

object LongAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

object LongSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Long? = when (data) {

    is Long -> data.toLong()

    is Int -> data.toLong()

    is Short -> data.toLong()

    is Byte -> data.toLong()

    is Number -> data.toDouble().let {
        if (it >= Long.MIN_VALUE && it <= Long.MAX_VALUE) {
            data.toLong()
        } else {
            null
        }
    }

    is String -> map(data.toDoubleOrNull())

    else -> null
}
