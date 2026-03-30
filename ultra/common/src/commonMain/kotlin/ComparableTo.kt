package io.peekandpoke.ultra.common

/**
 * Extension of [Comparable] that provides human-readable infix comparison operators.
 *
 * @param T The type this instance can be compared against.
 */
interface ComparableTo<T> : Comparable<T> {

    /** Returns `true` if this value is strictly greater than [other]. */
    infix fun isGreaterThan(other: T) = compareTo(other) > 0

    /** Returns `true` if this value is greater than or equal to [other]. */
    infix fun isGreaterThanOrEqualTo(other: T) = compareTo(other) >= 0

    /** Returns `true` if this value is equal to [other] according to [compareTo]. */
    infix fun isEqualTo(other: T) = compareTo(other) == 0

    /** Returns `true` if this value is less than or equal to [other]. */
    infix fun isLessThanOrEqualTo(other: T) = compareTo(other) <= 0

    /** Returns `true` if this value is strictly less than [other]. */
    infix fun isLessThan(other: T) = compareTo(other) < 0
}
