package de.peekandpoke.kraft.addons.forms.validation

class OrRule<T>(private val rules: List<Rule<T>>) : Rule<T> {

    override fun check(value: T): Boolean {
        return rules.any { it.check(value) }
    }

    override fun getMessage(value: T): String {
        return rules.map { it.getMessage(value) }
            .filter { it.isNotBlank() }
            .joinToString(" or ")
    }
}
