package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.joinOr
import io.peekandpoke.ultra.i18n.I18nTranslate

/** Composite rule that passes if any of the contained [rules] passes. */
class OrRule<T>(private val rules: List<Rule<T>>) : Rule<T> {

    override fun check(value: T): Boolean {
        return rules.any { it.check(value) }
    }

    override fun getMessage(value: T): String {
        return rules.map { it.getMessage(value) }
            .filter { it.isNotBlank() }
            .joinToString(" or ")
    }

    override fun getMessage(value: T, translate: I18nTranslate): String {
        return rules.map { it.getMessage(value, translate) }
            .filter { it.isNotBlank() }
            .joinToString(translate.forms.joinOr())
    }
}
