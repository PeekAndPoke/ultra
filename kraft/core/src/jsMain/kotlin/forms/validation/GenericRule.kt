package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.ultra.i18n.I18nTranslate

/** A [Rule] implementation backed by lambda functions for check and message. */
data class GenericRule<T>(
    private val checkFn: (value: T) -> Boolean,
    private val messageFn: (value: T) -> String = { "Invalid input" },
    /** Optional translation of the default message; `null` for a plain (non-translated) message. */
    private val i18nFn: ((value: T, translate: I18nTranslate) -> String)? = null,
) : Rule<T> {
    /** Replaces the message with a custom (non-translated) one. */
    operator fun invoke(message: (T) -> String): GenericRule<T> {
        return copy(messageFn = message, i18nFn = null)
    }

    override fun check(value: T): Boolean {
        return checkFn(value)
    }

    override fun getMessage(value: T, translate: I18nTranslate?): String {
        return translate?.let { i18nFn?.invoke(value, it) } ?: messageFn(value)
    }
}
