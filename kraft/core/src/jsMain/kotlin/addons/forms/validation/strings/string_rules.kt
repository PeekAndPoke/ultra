package de.peekandpoke.kraft.addons.forms.validation.strings

import de.peekandpoke.kraft.addons.forms.validation.GenericRule
import de.peekandpoke.kraft.addons.forms.validation.KraftFormsRuleDsl
import de.peekandpoke.kraft.addons.forms.validation.Rule

@KraftFormsRuleDsl
fun <T : CharSequence?> empty(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isEmpty() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> empty(message: String = "Must be empty"): Rule<T> =
    empty { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> notEmpty(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isNotEmpty() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> notEmpty(message: String = "Must not be empty"): Rule<T> =
    notEmpty { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> blank(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isBlank() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> blank(message: String = "Must be blank"): Rule<T> =
    blank { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> notBlank(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isNotBlank() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> notBlank(message: String = "Must not be blank"): Rule<T> =
    notBlank { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> minLength(length: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").length >= length }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> minLength(length: Int, message: String = "Must be at least $length characters"): Rule<T> =
    minLength(length) { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> maxLength(length: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").length <= length }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> maxLength(length: Int, message: String = "Must be at most $length characters"): Rule<T> =
    maxLength(length) { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> exactLength(length: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").length == length }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> exactLength(length: Int, message: String = "Must be $length characters"): Rule<T> =
    exactLength(length) { message }
