package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.ultra.i18n.I18nTranslate

/**
 * A [Rule] implementation backed by lambda functions for check and message.
 *
 * [checkFn] is `suspend` (so a rule can validate asynchronously); [messageFn]/[i18nFn] stay
 * synchronous — building an error string never needs to suspend.
 */
data class GenericRule<T>(
    private val checkFn: suspend (value: T) -> Boolean,
    private val messageFn: (value: T) -> String = { "Invalid input" },
    /** Optional translation of the default message; `null` for a plain (non-translated) message. */
    private val i18nFn: ((value: T, translate: I18nTranslate) -> String)? = null,
) : Rule<T> {
    /** Replaces the message with a custom (non-translated) one. */
    operator fun invoke(message: (T) -> String): GenericRule<T> {
        return copy(messageFn = message, i18nFn = null)
    }

    override suspend fun check(value: T): Boolean {
        return checkFn(value)
    }

    override suspend fun getMessage(value: T, translate: I18nTranslate?): String {
        return translate?.let { i18nFn?.invoke(value, it) } ?: messageFn(value)
    }
}
