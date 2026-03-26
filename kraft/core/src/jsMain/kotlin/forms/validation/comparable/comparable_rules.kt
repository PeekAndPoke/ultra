package io.peekandpoke.kraft.forms.validation.comparable

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule

/** Validates that the comparable value is within the range [from]..[to] (inclusive). */
@KraftFormsRuleDsl
fun <T : Comparable<T>> inRange(
    from: T,
    to: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it in from..to },
)

/** Validates that the comparable value is strictly greater than [value]. */
@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThan(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it > value },
)

/** Validates that the comparable value is greater than or equal to [value]. */
@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThanOrEqual(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it >= value }
)

/** Validates that the comparable value is strictly less than [value]. */
@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThan(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it < value }
)

/** Validates that the comparable value is less than or equal to [value]. */
@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThanOrEqual(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it <= value }
)
