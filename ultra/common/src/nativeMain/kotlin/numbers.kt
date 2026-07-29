package io.peekandpoke.ultra.common

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Scales into a `Long` and splits the digits by hand, so it always uses `.` as the separator.
 *
 * The scaled value must fit into a `Long`: `NaN` throws, and infinities or a
 * `value * 10^digits` above `Long.MAX_VALUE` silently saturate into a wrong result.
 */
internal actual fun Number.toFixedInternal(digits: Int): String {
    val dbl = toDouble()
    val factor = 10.0.pow(digits)
    val rounded = (abs(dbl) * factor).roundToLong()
    val intPart = rounded / factor.toLong()
    val fracPart = rounded % factor.toLong()

    val sign = if (dbl < 0.0) "-" else ""

    if (digits == 0) {
        return "$sign$intPart"
    }

    val fracStr = fracPart.toString().padStart(digits, '0')

    return "$sign$intPart.$fracStr"
}
