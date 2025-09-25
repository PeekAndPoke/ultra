package de.peekandpoke.kraft.forms.validation.numbers

import de.peekandpoke.kraft.forms.KraftFormsRuleDsl
import de.peekandpoke.kraft.forms.validation.GenericRule

@KraftFormsRuleDsl
fun <T : Number?> inRange(
    from: Number,
    to: Number,
    message: (T) -> String,
): de.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
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
): de.peekandpoke.kraft.forms.validation.Rule<T> =
    inRange(from, to) { message }

@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: (T) -> String,
): de.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() < (it ?: 0).toDouble() },
)

@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: String = "Must be greater than $value",
): de.peekandpoke.kraft.forms.validation.Rule<T> =
    greaterThan(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: (T) -> String,
): de.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() <= (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: String = "Must be greater than $value or equal",
): de.peekandpoke.kraft.forms.validation.Rule<T> =
    greaterThanOrEqual(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: (T) -> String,
): de.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() > (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: String = "Must be less than $value",
): de.peekandpoke.kraft.forms.validation.Rule<T> =
    lessThan(value) { message }

@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: (T) -> String,
): de.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() >= (it ?: 0).toDouble() }
)

@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: String = "Must be less than $value or equal",
): de.peekandpoke.kraft.forms.validation.Rule<T> =
    lessThanOrEqual(value) { message }
