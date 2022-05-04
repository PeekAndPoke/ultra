package de.peekandpoke.ultra.common

interface ComparedTo<T> : Comparable<T> {

    infix fun isGreaterThan(other: T) = compareTo(other) > 0

    infix fun isGreaterThanOrEqualTo(other: T) = compareTo(other) >= 0

    infix fun isEqualTo(other: T) = compareTo(other) == 0

    infix fun isLessThanOrEqualTo(other: T) = compareTo(other) <= 0

    infix fun isLessThan(other: T) = compareTo(other) < 0
}
