@file:JvmName("NumbersJvm")

package io.peekandpoke.ultra.common

import java.util.Locale

/**
 * Formats with [Locale.ROOT] so the decimal separator is always `.`, matching JS and native
 * regardless of the machine's default locale.
 *
 * Rounding is half-up applied to the shortest decimal form of the double, so `1.005.toFixed(2)`
 * is `1.01` here and `1.00` on JS and native. `NaN` and infinities render as `NaN`/`Infinity`.
 */
internal actual fun Number.toFixedInternal(digits: Int): String {
    return "%.${digits}f".format(Locale.ROOT, toDouble())
}
