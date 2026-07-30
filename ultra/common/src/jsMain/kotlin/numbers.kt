package io.peekandpoke.ultra.common

/**
 * Delegates to JS `Number.toFixed`, which always uses `.` as the separator and throws `RangeError`
 * for more than 100 digits. `NaN` and infinities render as `NaN`/`Infinity`.
 */
internal actual fun Number.toFixedInternal(digits: Int): String {
    @Suppress("UnsafeCastFromDynamic")
    return toDouble().asDynamic().toFixed(digits)
}
