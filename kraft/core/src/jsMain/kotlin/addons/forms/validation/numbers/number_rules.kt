package de.peekandpoke.kraft.addons.forms.validation.numbers

import de.peekandpoke.kraft.addons.forms.validation.GenericRule
import de.peekandpoke.kraft.addons.forms.validation.KraftFormsRuleDsl
import de.peekandpoke.kraft.addons.forms.validation.Rule

@KraftFormsRuleDsl
fun <T : Number?> inRange(
    from: Number,
    to: Number,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = {
        (it ?: 0).let { v -> from.toDouble() <= v.toDouble() && v.toDouble() <= to.toDouble() }
    },
)

@KraftFormsRuleDsl
fun <T : Number?> inRange(
    from: Number,
    to: Number,
    message: String = "Must be in range $from .. $to",
): Rule<T> =
    inRange(from, to) { message }

@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() < (it ?: 0).toDouble() },
)

@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: String = "Must be greater than $value",
): Rule<T> =
    greaterThan(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() <= (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: String = "Must be greater than $value or equal",
): Rule<T> =
    greaterThanOrEqual(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() > (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: String = "Must be less than $value",
): Rule<T> =
    lessThan(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: (T) -> String,
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() >= (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: String = "Must be less than $value or equal",
): Rule<T> =
    lessThanOrEqual(value) { message }
