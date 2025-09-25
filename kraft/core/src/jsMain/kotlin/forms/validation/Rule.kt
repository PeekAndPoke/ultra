package de.peekandpoke.kraft.forms.validation

interface Rule<T> {
    companion object

    fun check(value: T): Boolean

    fun getMessage(value: T): String
}

infix fun <T> Rule<T>.or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
