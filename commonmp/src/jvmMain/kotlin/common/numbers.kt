@file:JvmName("NumbersJvm")

package de.peekandpoke.ultra.common

internal actual fun Number.toFixedInternal(digits: Int): String {
    return "%.${digits}f".format(toDouble())
}
