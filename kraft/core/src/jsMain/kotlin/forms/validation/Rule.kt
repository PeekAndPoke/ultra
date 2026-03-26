package io.peekandpoke.kraft.forms.validation

/**
 * A validation rule that checks a value and produces an error message on failure.
 */
interface Rule<T> {
    companion object

    /** Returns true if [value] passes this rule. */
    fun check(value: T): Boolean

    /** Returns the error message to display when [value] fails this rule. */
    fun getMessage(value: T): String
}

/** Combines two rules with logical OR: passes if either rule passes. */
infix fun <T> Rule<T>.or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
