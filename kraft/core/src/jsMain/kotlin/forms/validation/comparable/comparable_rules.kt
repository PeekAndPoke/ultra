package io.peekandpoke.kraft.forms.validation.comparable

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule

@KraftFormsRuleDsl
fun <T : Comparable<T>> inRange(
    from: T,
    to: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it in from..to },
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThan(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it > value },
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThanOrEqual(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it >= value }
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThan(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it < value }
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThanOrEqual(
    value: T,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it <= value }
)
