package de.peekandpoke.kraft.addons.forms.validation.comparable

import de.peekandpoke.kraft.addons.forms.validation.GenericRule
import de.peekandpoke.kraft.addons.forms.validation.KraftFormsRuleDsl
import de.peekandpoke.kraft.addons.forms.validation.Rule

@KraftFormsRuleDsl
fun <T : Comparable<T>> inRange(
    from: T,
    to: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it in from..to },
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThan(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it > value },
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> greaterThanOrEqual(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it >= value }
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThan(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it < value }
)

@KraftFormsRuleDsl
fun <T : Comparable<T>> lessThanOrEqual(
    value: T,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it <= value }
)
