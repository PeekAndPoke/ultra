package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.ultra.i18n.I18nTranslate

/**
 * A validation rule that checks a value and produces an error message on failure.
 *
 * Both [check] and [getMessage] are `suspend` so a rule may validate against an async source (e.g.
 * asking the server whether a slug is still available). Pure/synchronous rules simply do not suspend.
 */
interface Rule<T> {

    /** Returns true if [value] passes this rule. May suspend for async validation. */
    suspend fun check(value: T): Boolean

    /**
     * Returns the error message when [value] fails this rule. With no [translate] the plain (default)
     * English message is returned; with a translation surface the built-in rules resolve their default
     * from the kraft i18n catalog, so errors translate and re-render on language switch. A call-site
     * custom message (string/lambda) is intentionally not translated.
     */
    suspend fun getMessage(value: T, translate: I18nTranslate? = null): String
}

/** Combines two rules with logical OR: passes if either rule passes. */
infix fun <T> Rule<T>.or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
