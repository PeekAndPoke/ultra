package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.ComparableTo
import kotlin.js.JsName

interface Operators {
    enum class Comparison {
        LT,
        LTE,
        EQ,
        GTE,
        GT;

        fun toHumanString(): String = when (this) {
            LT -> "<"
            LTE -> "<="
            EQ -> "=="
            GTE -> ">="
            GT -> ">"
        }

        @JsName("compare")
        operator fun <R, L : Comparable<R>> invoke(left: L, right: R): Boolean {
            return when (this) {
                LT -> left < right
                LTE -> left <= right
                EQ -> left == right
                GTE -> left >= right
                GT -> left > right
            }
        }

        @JsName("compareTo")
        operator fun <R, L : ComparableTo<R>> invoke(left: L, right: R): Boolean {
            return when (this) {
                LT -> left isLessThan right
                LTE -> left isLessThanOrEqualTo right
                EQ -> left isEqualTo right
                GTE -> left isGreaterThanOrEqualTo right
                GT -> left isGreaterThan right
            }
        }
    }
}
