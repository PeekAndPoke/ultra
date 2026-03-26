package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl

/** Creates a rule that passes if any of the given rules passes (logical OR). */
fun <T> anyRuleOf(rule: Rule<T>, vararg rules: Rule<T>): Rule<T> {
    val allRules = listOf(rule) + rules

    return GenericRule(
        checkFn = { value -> allRules.any { it.check(value) } },
        messageFn = { value ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value) }
                .filter { it.isNotBlank() }
                .joinToString(" or ")
        },
    )
}

/** Creates a rule that passes only if all given rules pass (logical AND). */
fun <T> allRulesOf(rule: Rule<T>, vararg rules: Rule<T>): Rule<T> {
    val allRules = listOf(rule) + rules

    return GenericRule(
        checkFn = { value -> allRules.all { it.check(value) } },
        messageFn = { value ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value) }
                .filter { it.isNotBlank() }
                .joinToString(" and ")
        },
    )
}

/** Validates that the value is not null. */
@KraftFormsRuleDsl
fun <T> nonNull(message: String = "Must not be empty"): Rule<T> =
    GenericRule(
        messageFn = { message },
        checkFn = { it != null },
    )

/** Passes if the value is null, otherwise delegates to [inner]. */
@KraftFormsRuleDsl
fun <T> nullOrElse(inner: Rule<T>): Rule<T?> =
    GenericRule(
        checkFn = { it == null || inner.check(it) },
        messageFn = {
            if (it == null) {
                "Invalid input"
            } else {
                inner.getMessage(it)
            }
        }
    )

/** Validates that the value is not null and passes the [inner] rule. */
@KraftFormsRuleDsl
fun <T> nonNullAnd(inner: Rule<T>): Rule<T?> =
    GenericRule(
        checkFn = { it != null && inner.check(it) },
        messageFn = {
            if (it == null) {
                "Must not be empty"
            } else {
                inner.getMessage(it)
            }
        }
    )

/** Validates that the value equals the result of [compareWith]. */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it == compareWith() }
    )

/** @see equalTo */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: String = "Must be equal to '$compareWith()'"): Rule<T> =
    equalTo(compareWith) { message }

/** @see equalTo */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: T, message: String = "Must be equal to '$compareWith()'"): Rule<T> =
    equalTo({ compareWith }) { message }

/** Validates that the value does not equal the result of [compareWith]. */
@KraftFormsRuleDsl
fun <T> notEqualTo(
    compareWith: () -> T,
    message: (T) -> String = { "Must not be equal to '$compareWith()'" },
) = GenericRule(
    messageFn = message,
    checkFn = { it != compareWith() }
)

/** @see notEqualTo */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: () -> T, message: String = "Must not be equal to '$compareWith()'"): Rule<T> =
    notEqualTo(compareWith) { message }

/** @see notEqualTo */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: T, message: String = "Must not be equal to '$compareWith()'"): Rule<T> =
    notEqualTo({ compareWith }) { message }

/** Validates that the value is contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> anyOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it in values() },
    )

/** @see anyOf */
@KraftFormsRuleDsl
fun <T> anyOf(values: Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    anyOf({ values }) { message }

/** Validates that the value is not contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it !in values() },
    )

/** @see noneOf */
@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    noneOf(values) { message }

/** @see noneOf */
@KraftFormsRuleDsl
fun <T> noneOf(values: Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    noneOf({ values }) { message }

/** Creates a rule from a custom [check] predicate and error [message]. */
@KraftFormsRuleDsl
fun <T> given(
    check: (T) -> Boolean,
    message: (T) -> String = { "Must be a valid input" },
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = check,
)
