package de.peekandpoke.ultra.common

import kotlin.math.pow
import kotlin.math.roundToLong

fun Number.toFixed(decimals: Int): String {

    if (decimals <= 0) {
        return this.toInt().toString()
    }

    val str1 = this.toString()

    return when (val dot = str1.indexOf('.')) {

        -1 -> "$str1.".padEnd(str1.length + 1 + decimals, '0')

        else -> str1.padEnd(dot + 1 + decimals, '0').substring(0, dot + 1 + decimals)
    }
}

fun Number.roundWithPrecision(precision: Int): Double {

    val thisDouble = this.toDouble()

    if (precision == 0) return thisDouble.roundToLong().toDouble()

    val factor = 10.0.pow(precision)

    return (thisDouble * factor).roundToLong() / factor
}

fun Double.roundWithPrecision(precision: Int): Double = (this as Number).roundWithPrecision(precision)

fun Float.roundWithPrecision(precision: Int): Float = (this as Number).roundWithPrecision(precision).toFloat()
