package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.i18n.generated.anyOf
import io.peekandpoke.kraft.i18n.generated.equalTo
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.invalidInput
import io.peekandpoke.kraft.i18n.generated.joinAnd
import io.peekandpoke.kraft.i18n.generated.joinOr
import io.peekandpoke.kraft.i18n.generated.nonNull
import io.peekandpoke.kraft.i18n.generated.noneOf
import io.peekandpoke.kraft.i18n.generated.notEqualTo

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
        i18nFn = { value, t ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value, t) }
                .filter { it.isNotBlank() }
                .joinToString(t.forms.joinOr())
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
        i18nFn = { value, t ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value, t) }
                .filter { it.isNotBlank() }
                .joinToString(t.forms.joinAnd())
        },
    )
}

/** Validates that the value is not null. */
@KraftFormsRuleDsl
fun <T> nonNull(): Rule<T> =
    GenericRule(
        checkFn = { it != null },
        messageFn = { "Must not be empty" },
        i18nFn = { _, t -> t.forms.nonNull() },
    )

/** @see nonNull */
@KraftFormsRuleDsl
fun <T> nonNull(message: String): Rule<T> =
    GenericRule(messageFn = { message }, checkFn = { it != null })

/** Passes if the value is null, otherwise delegates to [inner]. */
@KraftFormsRuleDsl
fun <T> nullOrElse(inner: Rule<T>): Rule<T?> =
    GenericRule(
        checkFn = { it == null || inner.check(it) },
        messageFn = { if (it == null) "Invalid input" else inner.getMessage(it) },
        i18nFn = { value, t -> if (value == null) t.forms.invalidInput() else inner.getMessage(value, t) },
    )

/** Validates that the value is not null and passes the [inner] rule. */
@KraftFormsRuleDsl
fun <T> nonNullAnd(inner: Rule<T>): Rule<T?> =
    GenericRule(
        checkFn = { it != null && inner.check(it) },
        messageFn = { if (it == null) "Must not be empty" else inner.getMessage(it) },
        i18nFn = { value, t -> if (value == null) t.forms.nonNull() else inner.getMessage(value, t) },
    )

/** Validates that the value equals the result of [compareWith] (custom message). */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it == compareWith() })

/** Validates that the value equals the result of [compareWith]. */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T): Rule<T> =
    GenericRule(
        checkFn = { it == compareWith() },
        // Never echo the operand — for a confirm-password field it is a secret (D-review).
        messageFn = { "The values must match" },
        i18nFn = { _, t -> t.forms.equalTo() },
    )

/** @see equalTo */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: String): Rule<T> =
    equalTo(compareWith) { message }

/** Validates that the value equals [compareWith]. */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: T): Rule<T> =
    equalTo({ compareWith })

/** @see equalTo */
@KraftFormsRuleDsl
fun <T> equalTo(compareWith: T, message: String): Rule<T> =
    equalTo({ compareWith }) { message }

/** Validates that the value does not equal the result of [compareWith] (custom message). */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: () -> T, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it != compareWith() })

/** Validates that the value does not equal the result of [compareWith]. */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: () -> T): Rule<T> =
    GenericRule(
        checkFn = { it != compareWith() },
        messageFn = { "The values must not match" },
        i18nFn = { _, t -> t.forms.notEqualTo() },
    )

/** @see notEqualTo */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: () -> T, message: String): Rule<T> =
    notEqualTo(compareWith) { message }

/** Validates that the value does not equal [compareWith]. */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: T): Rule<T> =
    notEqualTo({ compareWith })

/** @see notEqualTo */
@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: T, message: String): Rule<T> =
    notEqualTo({ compareWith }) { message }

/** Validates that the value is contained in the given [values] collection (custom message). */
@KraftFormsRuleDsl
fun <T> anyOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it in values() })

/** Validates that the value is contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> anyOf(values: () -> Collection<T>): Rule<T> =
    GenericRule(
        checkFn = { it in values() },
        messageFn = { "Must be a valid input" },
        i18nFn = { _, t -> t.forms.anyOf() },
    )

/** @see anyOf */
@KraftFormsRuleDsl
fun <T> anyOf(values: () -> Collection<T>, message: String): Rule<T> =
    anyOf(values) { message }

/** Validates that the value is contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> anyOf(values: Collection<T>): Rule<T> =
    anyOf({ values })

/** @see anyOf */
@KraftFormsRuleDsl
fun <T> anyOf(values: Collection<T>, message: String): Rule<T> =
    anyOf({ values }) { message }

/** Validates that the value is not contained in the given [values] collection (custom message). */
@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it !in values() })

/** Validates that the value is not contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>): Rule<T> =
    GenericRule(
        checkFn = { it !in values() },
        messageFn = { "Must be a valid input" },
        i18nFn = { _, t -> t.forms.noneOf() },
    )

/** @see noneOf */
@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: String): Rule<T> =
    noneOf(values) { message }

/** Validates that the value is not contained in the given [values] collection. */
@KraftFormsRuleDsl
fun <T> noneOf(values: Collection<T>): Rule<T> =
    noneOf({ values })

/** @see noneOf */
@KraftFormsRuleDsl
fun <T> noneOf(values: Collection<T>, message: String): Rule<T> =
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
