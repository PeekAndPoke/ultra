package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Number] values. */
object NumberAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Number] values. */
object NumberSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

private fun map(data: Any?): Number? = when (data) {

    is Number -> data

    is String -> data.toDoubleOrNull()

    else -> null
}
