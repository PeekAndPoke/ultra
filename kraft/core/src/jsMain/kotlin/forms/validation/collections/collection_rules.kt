package io.peekandpoke.kraft.forms.validation.collections

import io.peekandpoke.kraft.forms.KraftFormsRuleDsl
import io.peekandpoke.kraft.forms.validation.GenericRule
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.i18n.generated.collectionNotEmpty
import io.peekandpoke.kraft.i18n.generated.exactCount
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.maxCount
import io.peekandpoke.kraft.i18n.generated.minCount

/** Validates that the collection is not empty (custom message). */
@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it.isNotEmpty() })

/** Validates that the collection is not empty. */
@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(): Rule<T> =
    GenericRule(
        checkFn = { it.isNotEmpty() },
        messageFn = { "Must not be empty" },
        i18nFn = { _, t -> t.forms.collectionNotEmpty() },
    )

/** @see notEmpty */
@KraftFormsRuleDsl
fun <T : Collection<*>> notEmpty(message: String): Rule<T> =
    notEmpty { message }

/** Validates that the collection has at least [count] items (custom message). */
@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it.size >= count })

/** Validates that the collection has at least [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int): Rule<T> =
    GenericRule(
        checkFn = { it.size >= count },
        messageFn = { "Must have at least $count items" },
        i18nFn = { _, t -> t.forms.minCount(count = count) },
    )

/** @see minCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> minCount(count: Int, message: String): Rule<T> =
    minCount(count) { message }

/** Validates that the collection has at most [count] items (custom message). */
@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it.size <= count })

/** Validates that the collection has at most [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int): Rule<T> =
    GenericRule(
        checkFn = { it.size <= count },
        messageFn = { "Must have at most $count items" },
        i18nFn = { _, t -> t.forms.maxCount(count = count) },
    )

/** @see maxCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> maxCount(count: Int, message: String): Rule<T> =
    maxCount(count) { message }

/** Validates that the collection has exactly [count] items (custom message). */
@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: (T) -> String): Rule<T> =
    GenericRule(messageFn = message, checkFn = { it.size == count })

/** Validates that the collection has exactly [count] items. */
@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int): Rule<T> =
    GenericRule(
        checkFn = { it.size == count },
        messageFn = { "Must have $count items" },
        i18nFn = { _, t -> t.forms.exactCount(count = count) },
    )

/** @see exactCount */
@KraftFormsRuleDsl
fun <T : Collection<*>> exactCount(count: Int, message: String): Rule<T> =
    exactCount(count) { message }
