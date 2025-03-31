package de.peekandpoke.kraft.addons.forms.validation

data class GenericRule<T>(
    private val checkFn: (value: T) -> Boolean,
    private val messageFn: (value: T) -> String = { "Invalid input" },
) : Rule<T> {
    operator fun invoke(message: (T) -> String): GenericRule<T> {
        return copy(messageFn = message)
    }

    override fun check(value: T): Boolean {
        return checkFn(value)
    }

    override fun getMessage(value: T): String {
        return messageFn(value)
    }
}
