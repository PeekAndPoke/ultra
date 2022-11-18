package de.peekandpoke.ultra.common

internal actual fun Number.toFixedInternal(digits: Int): String {
    @Suppress("UnsafeCastFromDynamic")
    return toDouble().asDynamic().toFixed(digits)
}
