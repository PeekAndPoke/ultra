package io.peekandpoke.kraft.forms.validation.numbers

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule

/** Validates that the number is within the range [from]..[to] (inclusive). */
@KraftFormsRuleDsl
fun <T : Number?> inRange(
    from: Number,
    to: Number,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = {
        (it ?: 0).let { v -> from.toDouble() <= v.toDouble() && v.toDouble() <= to.toDouble() }
    },
)

/** @see inRange */
@KraftFormsRuleDsl
fun <T : Number?> inRange(
    from: Number,
    to: Number,
    message: String = "Must be in range $from .. $to",
): io.peekandpoke.kraft.forms.validation.Rule<T> =
    inRange(from, to) { message }

/** Validates that the number is strictly greater than [value]. */
@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() < (it ?: 0).toDouble() },
)

/** @see greaterThan */
@KraftFormsRuleDsl
fun <T : Number?> greaterThan(
    value: Number,
    message: String = "Must be greater than $value",
): io.peekandpoke.kraft.forms.validation.Rule<T> =
    greaterThan(value) { message }

/** Validates that the number is greater than or equal to [value]. */
@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() <= (it ?: 0).toDouble() }
)

/** @see greaterThanOrEqual */
@KraftFormsRuleDsl
fun <T : Number?> greaterThanOrEqual(
    value: Number,
    message: String = "Must be greater than $value or equal",
): io.peekandpoke.kraft.forms.validation.Rule<T> =
    greaterThanOrEqual(value) { message }

/** Validates that the number is strictly less than [value]. */
@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() > (it ?: 0).toDouble() }
)

/** @see lessThan */
@KraftFormsRuleDsl
fun <T : Number?> lessThan(
    value: Number,
    message: String = "Must be less than $value",
): io.peekandpoke.kraft.forms.validation.Rule<T> =
    lessThan(value) { message }

/** Validates that the number is less than or equal to [value]. */
@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: (T) -> String,
): io.peekandpoke.kraft.forms.validation.Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { value.toDouble() >= (it ?: 0).toDouble() }
)

/** @see lessThanOrEqual */
@KraftFormsRuleDsl
fun <T : Number?> lessThanOrEqual(
    value: Number,
    message: String = "Must be less than $value or equal",
): io.peekandpoke.kraft.forms.validation.Rule<T> =
    lessThanOrEqual(value) { message }
