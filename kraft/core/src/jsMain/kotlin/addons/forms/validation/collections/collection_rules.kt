package de.peekandpoke.kraft.addons.forms.validation.collections

import de.peekandpoke.kraft.addons.forms.validation.GenericRule
import de.peekandpoke.kraft.addons.forms.validation.KraftFormsRuleDsl
import de.peekandpoke.kraft.addons.forms.validation.Rule

@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it.isNotEmpty() }
)

@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: String = "Must not be empty"): Rule<T> =
    notEmpty { message }

@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it.size >= count }
)

@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: String = "Must have at least $count items"): Rule<T> =
    minCount(count) { message }

@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it.size >= count }
)

@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: String = "Must have at most $count items"): Rule<T> =
    maxCount(count) { message }

@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it.size == count }
)

@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: String = "Must have $count items"): Rule<T> =
    maxCount(count) { message }
