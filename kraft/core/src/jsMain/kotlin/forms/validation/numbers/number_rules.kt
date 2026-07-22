package io.peekandpoke.kraft.forms.validation.numbers

import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.greaterThan
import io.peekandpoke.kraft.i18n.generated.greaterThanOrEqual
import io.peekandpoke.kraft.i18n.generated.inRange
import io.peekandpoke.kraft.i18n.generated.lessThan
import io.peekandpoke.kraft.i18n.generated.lessThanOrEqual

/** Validates that the number is within the range [from]..[to] (inclusive) (custom message). */
fun <T : Number?> inRange(from: Number, to: Number, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { (it ?: 0).let { v -> from.toDouble() <= v.toDouble() && v.toDouble() <= to.toDouble() } },
    )

/** Validates that the number is within the range [from]..[to] (inclusive). */
fun <T : Number?> inRange(from: Number, to: Number): Rule<T> =
    GenericRule(
        checkFn = { (it ?: 0).let { v -> from.toDouble() <= v.toDouble() && v.toDouble() <= to.toDouble() } },
        messageFn = { "Must be in range $from .. $to" },
        i18nFn = { _, t -> t.forms.inRange(from = from, to = to) },
    )

/** @see inRange */
fun <T : Number?> inRange(from: Number, to: Number, message: String): Rule<T> =
    inRange(from, to) { message }

/** Validates that the number is strictly greater than [value] (custom message). */
fun <T : Number?> greaterThan(value: Number, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { value.toDouble() < (it ?: 0).toDouble() })

/** Validates that the number is strictly greater than [value]. */
fun <T : Number?> greaterThan(value: Number): Rule<T> =
    GenericRule(
        checkFn = { value.toDouble() < (it ?: 0).toDouble() },
        messageFn = { "Must be greater than $value" },
        i18nFn = { _, t -> t.forms.greaterThan(limit = value) },
    )

/** @see greaterThan */
fun <T : Number?> greaterThan(value: Number, message: String): Rule<T> =
    greaterThan(value) { message }

/** Validates that the number is greater than or equal to [value] (custom message). */
fun <T : Number?> greaterThanOrEqual(value: Number, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { value.toDouble() <= (it ?: 0).toDouble() })

/** Validates that the number is greater than or equal to [value]. */
fun <T : Number?> greaterThanOrEqual(value: Number): Rule<T> =
    GenericRule(
        checkFn = { value.toDouble() <= (it ?: 0).toDouble() },
        messageFn = { "Must be greater than $value or equal" },
        i18nFn = { _, t -> t.forms.greaterThanOrEqual(limit = value) },
    )

/** @see greaterThanOrEqual */
fun <T : Number?> greaterThanOrEqual(value: Number, message: String): Rule<T> =
    greaterThanOrEqual(value) { message }

/** Validates that the number is strictly less than [value] (custom message). */
fun <T : Number?> lessThan(value: Number, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { value.toDouble() > (it ?: 0).toDouble() })

/** Validates that the number is strictly less than [value]. */
fun <T : Number?> lessThan(value: Number): Rule<T> =
    GenericRule(
        checkFn = { value.toDouble() > (it ?: 0).toDouble() },
        messageFn = { "Must be less than $value" },
        i18nFn = { _, t -> t.forms.lessThan(limit = value) },
    )

/** @see lessThan */
fun <T : Number?> lessThan(value: Number, message: String): Rule<T> =
    lessThan(value) { message }

/** Validates that the number is less than or equal to [value] (custom message). */
fun <T : Number?> lessThanOrEqual(value: Number, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { value.toDouble() >= (it ?: 0).toDouble() })

/** Validates that the number is less than or equal to [value]. */
fun <T : Number?> lessThanOrEqual(value: Number): Rule<T> =
    GenericRule(
        checkFn = { value.toDouble() >= (it ?: 0).toDouble() },
        messageFn = { "Must be less than $value or equal" },
        i18nFn = { _, t -> t.forms.lessThanOrEqual(limit = value) },
    )

/** @see lessThanOrEqual */
fun <T : Number?> lessThanOrEqual(value: Number, message: String): Rule<T> =
    lessThanOrEqual(value) { message }
