package io.peekandpoke.kraft.forms.validation.strings

import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.i18n.generated.blank
import io.peekandpoke.kraft.i18n.generated.empty
import io.peekandpoke.kraft.i18n.generated.exactLength
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.maxLength
import io.peekandpoke.kraft.i18n.generated.minLength
import io.peekandpoke.kraft.i18n.generated.notBlank
import io.peekandpoke.kraft.i18n.generated.notEmpty

/** Validates that the string is empty (custom message). */
fun <T : CharSequence?> empty(message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").isEmpty() }, messageFn = message)

/** Validates that the string is empty. */
fun <T : CharSequence?> empty(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isEmpty() },
        messageFn = { "Must be empty" },
        i18nFn = { _, t -> t.forms.empty() },
    )

/** @see empty */
fun <T : CharSequence?> empty(message: String): Rule<T> =
    empty { message }

/** Validates that the string is not empty (custom message). */
fun <T : CharSequence?> notEmpty(message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").isNotEmpty() }, messageFn = message)

/** Validates that the string is not empty. */
fun <T : CharSequence?> notEmpty(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isNotEmpty() },
        messageFn = { "Must not be empty" },
        i18nFn = { _, t -> t.forms.notEmpty() },
    )

/** @see notEmpty */
fun <T : CharSequence?> notEmpty(message: String): Rule<T> =
    notEmpty { message }

/** Validates that the string is blank (empty or whitespace only) (custom message). */
fun <T : CharSequence?> blank(message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").isBlank() }, messageFn = message)

/** Validates that the string is blank (empty or whitespace only). */
fun <T : CharSequence?> blank(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isBlank() },
        messageFn = { "Must be blank" },
        i18nFn = { _, t -> t.forms.blank() },
    )

/** @see blank */
fun <T : CharSequence?> blank(message: String): Rule<T> =
    blank { message }

/** Validates that the string is not blank (custom message). */
fun <T : CharSequence?> notBlank(message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").isNotBlank() }, messageFn = message)

/** Validates that the string is not blank. */
fun <T : CharSequence?> notBlank(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isNotBlank() },
        messageFn = { "Must not be blank" },
        i18nFn = { _, t -> t.forms.notBlank() },
    )

/** @see notBlank */
fun <T : CharSequence?> notBlank(message: String): Rule<T> =
    notBlank { message }

/** Validates that the string has at least [length] characters (custom message). */
fun <T : CharSequence?> minLength(length: Int, message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").length >= length }, messageFn = message)

/** Validates that the string has at least [length] characters. */
fun <T : CharSequence?> minLength(length: Int): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").length >= length },
        messageFn = { "Must be at least $length characters" },
        i18nFn = { _, t -> t.forms.minLength(count = length) },
    )

/** @see minLength */
fun <T : CharSequence?> minLength(length: Int, message: String): Rule<T> =
    minLength(length) { message }

/** Validates that the string has at most [length] characters (custom message). */
fun <T : CharSequence?> maxLength(length: Int, message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").length <= length }, messageFn = message)

/** Validates that the string has at most [length] characters. */
fun <T : CharSequence?> maxLength(length: Int): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").length <= length },
        messageFn = { "Must be at most $length characters" },
        i18nFn = { _, t -> t.forms.maxLength(count = length) },
    )

/** @see maxLength */
fun <T : CharSequence?> maxLength(length: Int, message: String): Rule<T> =
    maxLength(length) { message }

/** Validates that the string has exactly [length] characters (custom message). */
fun <T : CharSequence?> exactLength(length: Int, message: (T) -> String): Rule<T> =
    GenericRule(checkFn = { (it ?: "").length == length }, messageFn = message)

/** Validates that the string has exactly [length] characters. */
fun <T : CharSequence?> exactLength(length: Int): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").length == length },
        messageFn = { "Must be $length characters" },
        i18nFn = { _, t -> t.forms.exactLength(count = length) },
    )

/** @see exactLength */
fun <T : CharSequence?> exactLength(length: Int, message: String): Rule<T> =
    exactLength(length) { message }
