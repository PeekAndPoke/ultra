package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Int] values. */
object IntAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Int] values. */
object IntSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Int]: an existing [Int] is returned as-is, a [Short] or [Byte] widens
 * directly, other [Number]s are range-checked and truncated, and a [String] is parsed as a
 * [Double] and converted the same way. Out-of-range values and anything else map to `null`.
 */
private fun map(data: Any?): Int? = when (data) {

    is Int -> data

    is Short -> data.toInt()

    is Byte -> data.toInt()

    // TODO(scan): Double.toLong() returns 0 for NaN, so a NaN input (e.g. the string "NaN") passes the range check and silently becomes 0 instead of null.
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
