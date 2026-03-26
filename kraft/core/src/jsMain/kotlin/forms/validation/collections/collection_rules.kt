package io.peekandpoke.kraft.forms.validation.collections

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule

/** Validates that the collection is not empty. */
@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it.isNotEmpty() }
    )

/** @see notEmpty */
@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: String = "Must not be empty"): Rule<T> =
    notEmpty { message }

/** Validates that the collection has at least [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it.size >= count }
    )

/** @see minCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: String = "Must have at least $count items"): Rule<T> =
    minCount(count) { message }

/** Validates that the collection has at most [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it.size <= count }
    )

/** @see maxCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: String = "Must have at most $count items"): Rule<T> =
    maxCount(count) { message }

/** Validates that the collection has exactly [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(
        messageFn = message,
        checkFn = { it.size == count }
    )

/** @see exactCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: String = "Must have $count items"): Rule<T> =
    exactCount(count) { message }
