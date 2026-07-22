package io.peekandpoke.kraft.forms.validation.comparable

import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule

/** Validates that the comparable value is within the range [from]..[to] (inclusive). */
fun <T : Comparable<T>> inRange(
    from: T,
    to: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it in from..to },
)

/** Validates that the comparable value is strictly greater than [value]. */
fun <T : Comparable<T>> greaterThan(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it > value },
)

/** Validates that the comparable value is greater than or equal to [value]. */
fun <T : Comparable<T>> greaterThanOrEqual(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it >= value }
)

/** Validates that the comparable value is strictly less than [value]. */
fun <T : Comparable<T>> lessThan(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it < value }
)

/** Validates that the comparable value is less than or equal to [value]. */
fun <T : Comparable<T>> lessThanOrEqual(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it <= value }
)
