package io.peekandpoke.kraft.forms.validation.strings

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.Rule

/** Validates that the string is empty. */
@KraftFormsRuleDsl
fun <T : CharSequence?> empty(message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isEmpty() }
    )

/** @see empty */
@KraftFormsRuleDsl
fun <T : CharSequence?> empty(message: String = "Must be empty"): Rule<T> =
    empty { message }

/** Validates that the string is not empty. */
@KraftFormsRuleDsl
fun <T : CharSequence?> notEmpty(message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotEmpty() }
    )

/** @see notEmpty */
@KraftFormsRuleDsl
fun <T : CharSequence?> notEmpty(message: String = "Must not be empty"): Rule<T> =
    notEmpty { message }

/** Validates that the string is blank (empty or whitespace only). */
@KraftFormsRuleDsl
fun <T : CharSequence?> blank(message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isBlank() }
    )

/** @see blank */
@KraftFormsRuleDsl
fun <T : CharSequence?> blank(message: String = "Must be blank"): Rule<T> =
    blank { message }

/** Validates that the string is not blank. */
@KraftFormsRuleDsl
fun <T : CharSequence?> notBlank(message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotBlank() }
    )

/** @see notBlank */
@KraftFormsRuleDsl
fun <T : CharSequence?> notBlank(message: String = "Must not be blank"): Rule<T> =
    notBlank { message }

/** Validates that the string has at least [length] characters. */
@KraftFormsRuleDsl
fun <T : CharSequence?> minLength(length: Int, message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").length >= length }
    )

/** @see minLength */
@KraftFormsRuleDsl
fun <T : CharSequence?> minLength(length: Int, message: String = "Must be at least $length characters"): Rule<T> =
    minLength(length) { message }

/** Validates that the string has at most [length] characters. */
@KraftFormsRuleDsl
fun <T : CharSequence?> maxLength(length: Int, message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").length <= length }
    )

/** @see maxLength */
@KraftFormsRuleDsl
fun <T : CharSequence?> maxLength(length: Int, message: String = "Must be at most $length characters"): Rule<T> =
    maxLength(length) { message }

/** Validates that the string has exactly [length] characters. */
@KraftFormsRuleDsl
fun <T : CharSequence?> exactLength(length: Int, message: (T) -> String): Rule<T> =
    _root_ide_package_.io.peekandpoke.kraft.forms.validation.GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").length == length }
    )

/** @see exactLength */
@KraftFormsRuleDsl
fun <T : CharSequence?> exactLength(length: Int, message: String = "Must be $length characters"): Rule<T> =
    exactLength(length) { message }
