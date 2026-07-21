package io.peekandpoke.kraft.forms.validation.strings

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.validEmail
import io.peekandpoke.kraft.i18n.generated.validSlug
import io.peekandpoke.kraft.i18n.generated.validUrl
import io.peekandpoke.ultra.common.isEmail
import io.peekandpoke.ultra.common.isSlug
import io.peekandpoke.ultra.common.isUrlWithProtocol

/** Validates that the string is a valid email address (custom message). */
@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isEmail() },
    )

/** Validates that the string is a valid email address. */
@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isEmail() },
        messageFn = { "Must be a valid email" },
        i18nFn = { _, t -> t.forms.validEmail() },
    )

/** @see validEmail */
@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: String): Rule<T> =
    validEmail { message }

/** Validates that the string is a valid URL including the protocol (custom message). */
@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isUrlWithProtocol() },
    )

/** Validates that the string is a valid URL including the protocol. */
@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(): Rule<T> =
    GenericRule(
        checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isUrlWithProtocol() },
        messageFn = { "Must be a valid url" },
        i18nFn = { _, t -> t.forms.validUrl() },
    )

/** @see validUrlWithProtocol */
@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: String): Rule<T> =
    validUrlWithProtocol { message }

/**
 * Validates that the string is a valid slug / DNS label (lowercase letters, digits and hyphens,
 * no leading/trailing hyphen, length 1..63). Useful for tenant slugs that double as subdomains.
 * (custom message)
 */
@KraftFormsRuleDsl
fun <T : CharSequence?> validSlug(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it != null && it.toString().isSlug() },
    )

/** @see validSlug */
@KraftFormsRuleDsl
fun <T : CharSequence?> validSlug(): Rule<T> =
    GenericRule(
        checkFn = { it != null && it.toString().isSlug() },
        messageFn = { "Must be lowercase letters, digits and hyphens (no leading or trailing hyphen)" },
        i18nFn = { _, t -> t.forms.validSlug() },
    )

/** @see validSlug */
@KraftFormsRuleDsl
fun <T : CharSequence?> validSlug(message: String): Rule<T> =
    validSlug { message }
