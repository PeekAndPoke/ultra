package io.peekandpoke.kraft.forms.validation.strings

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.ultra.common.isEmail
import io.peekandpoke.ultra.common.isUrlWithProtocol

/** Validates that the string is a valid email address. */
@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isEmail() }
    )

/** @see validEmail */
@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: String = "Must be a valid email"): Rule<T> =
    validEmail { message }

/** Validates that the string is a valid URL including the protocol. */
@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isUrlWithProtocol() }
    )

/** @see validUrlWithProtocol */
@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: String = "Must be a valid url"): Rule<T> =
    validUrlWithProtocol { message }
