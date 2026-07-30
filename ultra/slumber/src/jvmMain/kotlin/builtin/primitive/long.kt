package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Long] values. */
object LongAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Long] values. */
object LongSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Long]: an existing [Long] is returned as-is, narrower integer types
 * widen directly, other [Number]s are range-checked via [Double] and converted, and a [String] is
 * parsed as a [Double] and converted the same way. Out-of-range values and anything else map to
 * `null`.
 */
private fun map(data: Any?): Long? = when (data) {

    is Long -> data

    is Int -> data.toLong()

    is Short -> data.toLong()

    is Byte -> data.toLong()

    // TODO(scan): Long.MAX_VALUE widens to 9223372036854775808.0 (2^63) when compared as a Double, so that out-of-range value passes the check and saturates to Long.MAX_VALUE instead of null.
    is Number -> data.toDouble().let {
        if (it >= Long.MIN_VALUE && it <= Long.MAX_VALUE) {
            data.toLong()
        } else {
            null
        }
    }

    // TODO(scan): routing through Double loses precision above 2^53; e.g. "9007199254740993" silently awakens as 9007199254740992L instead of matching or failing.
    is String -> map(data.toDoubleOrNull())

    else -> null
}
