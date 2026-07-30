package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [String] values. */
object StringAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [String] values. */
object StringSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [String]: an existing [String] is returned as-is, and [Number], [Char],
 * or [Boolean] are converted via `toString()`; anything else maps to `null`.
 */
private fun map(data: Any?): String? = when (data) {

    is String -> data

    is Number -> data.toString()

    is Char -> data.toString()

    is Boolean -> data.toString()

    else -> null
}
