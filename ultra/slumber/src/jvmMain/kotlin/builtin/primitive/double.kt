package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Double] values. */
object DoubleAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Double] values. */
object DoubleSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Double]: any [Number] widens via `toDouble()` and a [String] is
 * parsed via [String.toDoubleOrNull]; anything else maps to `null`.
 */
private fun map(data: Any?): Double? = when (data) {

    is Number -> data.toDouble()

    is String -> data.toDoubleOrNull()

    else -> null
}
