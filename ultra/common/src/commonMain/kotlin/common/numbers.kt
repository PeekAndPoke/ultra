package de.peekandpoke.ultra.common

import kotlin.math.pow
import kotlin.math.roundToLong

fun Number.toFixed(digits: Int): String {
    if (digits < 0) {
        return toInt().toString()
    }

    return toFixedInternal(digits)
}

internal expect fun Number.toFixedInternal(digits: Int): String

// fun Number.toFixed(decimals: Int): String {
//
//    if (decimals <= 0) {
//        return toInt().toString()
//    }
//
//    val dbl = toDouble()
//    val signum = if (dbl >= 0.0) "" else "-"
//
//    val dblAbs = abs(dbl)
//    val beforeComma = dblAbs.toInt()
//    var rest = (dblAbs - beforeComma)
//    val afterComma = Array(decimals) { "0" }
//    var allZeros = beforeComma == 0
//
//    repeat(decimals) { idx ->
//        rest *= 10
//        val i = rest.toInt()
//        if (i > 0) {
//            afterComma[idx] = i.toString()
//            allZeros = false
//        }
//        rest -= i
//    }
//
//    return "${if (allZeros) "" else signum}$beforeComma.${afterComma.joinToString("")}"
// }

fun Number.roundWithPrecision(precision: Int): Double {

    val thisDouble = this.toDouble()

    if (precision == 0) return thisDouble.roundToLong().toDouble()

    val factor = 10.0.pow(precision)

    return (thisDouble * factor).roundToLong() / factor
}

fun Double.roundWithPrecision(precision: Int): Double = (this as Number).roundWithPrecision(precision)

fun Float.roundWithPrecision(precision: Int): Float = (this as Number).roundWithPrecision(precision).toFloat()
