package io.peekandpoke.ultra.common

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

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
