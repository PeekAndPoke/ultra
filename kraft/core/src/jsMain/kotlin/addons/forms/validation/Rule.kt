package de.peekandpoke.kraft.addons.forms.validation

interface Rule<T> {
    companion object

    fun check(value: T): Boolean

    fun getMessage(value: T): String

    infix fun or(other: Rule<T>): Rule<T> = OrRule(listOf(this, other))
}
