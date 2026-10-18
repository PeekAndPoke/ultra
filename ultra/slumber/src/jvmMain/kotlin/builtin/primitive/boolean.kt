package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Boolean] values. */
object BooleanAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Boolean] values. */
object BooleanSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Boolean]: an existing [Boolean] is returned as-is, a [Number] is
 * non-zero-checked via `toInt()`, and a [String] is checked via [String.toBoolean] or a non-zero
 * [String.toIntOrNull]. Anything else maps to `null`.
 */
private fun map(data: Any?): Boolean? = when (data) {

    is Boolean -> data

    // TODO(scan): Long.toInt() truncates bits (not saturating); exact multiples of 2^32 collapse to 0 and are misreported as false.
    is Number -> data.toInt() != 0

    // TODO(scan): fractional or out-of-Int32-range numeric strings always fall through to false, unlike the equivalent raw Number.
    is String -> data.toBoolean() || when (data.toIntOrNull()) {
        null -> false
        0 -> false
        else -> true
    }

    else -> null
}
