package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Byte] values. */
object ByteAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Byte] values. */
object ByteSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Byte]: an existing [Byte] is returned as-is, other [Number]s are
 * range-checked and truncated, and a [String] is parsed as a [Double] and converted the same way.
 * Out-of-range values and anything else map to `null`.
 */
private fun map(data: Any?): Byte? = when (data) {

    is Byte -> data

    // TODO(scan): Double.toLong() returns 0 for NaN, so a NaN input (e.g. the string "NaN") passes the range check and silently becomes 0 instead of null.
    is Number -> data.toLong().let {
        if (it >= Byte.MIN_VALUE && it <= Byte.MAX_VALUE) {
            data.toByte()
        } else {
            null
        }
    }

    is String -> map(data.toDoubleOrNull())

    else -> null
}
