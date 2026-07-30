package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Short] values. */
object ShortAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Short] values. */
object ShortSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Short]: an existing [Short] is returned as-is, a [Byte] widens
 * directly, other [Number]s are range-checked and truncated, and a [String] is parsed as a
 * [Double] and converted the same way. Out-of-range values and anything else map to `null`.
 */
private fun map(data: Any?): Short? = when (data) {

    is Short -> data

    is Byte -> data.toShort()

    // TODO(scan): Double.toLong() returns 0 for NaN, so a NaN input (e.g. the string "NaN") passes the range check and silently becomes 0 instead of null.
    is Number -> data.toLong().let {
        if (it >= Short.MIN_VALUE && it <= Short.MAX_VALUE) {
            data.toShort()
        } else {
            null
        }
    }

    is String -> map(data.toDoubleOrNull())

    else -> null
}
