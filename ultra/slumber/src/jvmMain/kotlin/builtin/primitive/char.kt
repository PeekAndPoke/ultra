package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Char] values. */
object CharAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Char] values. */
object CharSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Char]: an existing [Char] is returned as-is, and a non-empty [String]
 * yields its first character (silently discarding the rest); anything else maps to `null`.
 */
private fun map(data: Any?): Char? = when {

    data is Char -> data

    data is String && data.length > 0 -> data[0]

    else -> null
}
