package io.peekandpoke.ultra.slumber.builtin.primitive

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Float] values. */
object FloatAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = map(data)
}

/** Slumberer for [Float] values. */
object FloatSlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = map(data)
}

/**
 * Best-effort conversion to [Float]: any [Number] narrows via `toFloat()` and a [String] is parsed
 * via [String.toFloatOrNull]; anything else maps to `null`.
 */
private fun map(data: Any?): Float? = when (data) {

    // TODO(scan): no range check before narrowing; a Double outside Float's range (e.g. Double.MAX_VALUE) silently becomes Float.POSITIVE_INFINITY instead of null, unlike byte/short/int/long's explicit range checks.
    is Number -> data.toFloat()

    is String -> data.toFloatOrNull()

    else -> null
}
