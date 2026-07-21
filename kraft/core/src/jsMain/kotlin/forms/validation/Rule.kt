package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.ultra.i18n.I18nTranslate

/**
 * A validation rule that checks a value and produces an error message on failure.
 */
interface Rule<T> {
    companion object

    /** Returns true if [value] passes this rule. */
    fun check(value: T): Boolean

    /**
     * Returns the error message when [value] fails this rule. With no [translate] the plain (default)
     * English message is returned; with a translation surface the built-in rules resolve their default
     * from the kraft i18n catalog, so errors translate and re-render on language switch. A call-site
     * custom message (string/lambda) is intentionally not translated.
     */
    fun getMessage(value: T, translate: I18nTranslate? = null): String
}

/** Combines two rules with logical OR: passes if either rule passes. */
infix fun <T> Rule<T>.or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
