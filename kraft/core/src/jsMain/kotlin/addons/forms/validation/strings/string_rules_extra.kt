package de.peekandpoke.kraft.addons.forms.validation.strings

import de.peekandpoke.kraft.addons.forms.validation.GenericRule
import de.peekandpoke.kraft.addons.forms.validation.KraftFormsRuleDsl
import de.peekandpoke.kraft.addons.forms.validation.Rule
import de.peekandpoke.ultra.common.isEmail
import de.peekandpoke.ultra.common.isUrlWithProtocol

@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isEmail() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> validEmail(message: String = "Must be a valid email"): Rule<T> =
    validEmail { message }

@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { (it ?: "").isNotBlank() && (it ?: "").toString().isUrlWithProtocol() }
)

@KraftFormsRuleDsl
fun <T : CharSequence?> validUrlWithProtocol(message: String = "Must be a valid url"): Rule<T> =
    validUrlWithProtocol { message }
