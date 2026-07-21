package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.ultra.i18n.I18nTranslate

/**
 * A validation rule that checks a value and produces an error message on failure.
 */
interface Rule<T> {
    companion object

    /** Returns true if [value] passes this rule. */
    fun check(value: T): Boolean

    /** Returns the plain (default, non-translated) error message when [value] fails this rule. */
    fun getMessage(value: T): String

    /**
     * Returns the error message resolved against the current [translate] surface. Defaults to the
     * plain [getMessage]; the built-in rules override it to resolve their default message from the
     * kraft i18n catalog, so validation errors translate and re-render on language switch. A call-site
     * custom message (string/lambda) is intentionally not translated — it falls through to [getMessage].
     */
    fun getMessage(value: T, translate: I18nTranslate): String = getMessage(value)
}

/** Combines two rules with logical OR: passes if either rule passes. */
infix fun <T> Rule<T>.or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
