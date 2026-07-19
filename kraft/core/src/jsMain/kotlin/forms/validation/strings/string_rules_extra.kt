package io.peekandpoke.kraft.forms.validation.strings

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.ultra.common.isEmail
import io.peekandpoke.ultra.common.isSlug
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

/**
 * Validates that the string is a valid slug / DNS label (lowercase letters, digits and hyphens,
 * no leading/trailing hyphen, length 1..63). Useful for tenant slugs that double as subdomains.
 */
@KraftFormsRuleDsl
fun <T : CharSequence?> validSlug(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it != null && it.toString().isSlug() }
    )

/** @see validSlug */
@KraftFormsRuleDsl
fun <T : CharSequence?> validSlug(
    message: String = "Must be lowercase letters, digits and hyphens (no leading or trailing hyphen)",
): Rule<T> =
    validSlug { message }
