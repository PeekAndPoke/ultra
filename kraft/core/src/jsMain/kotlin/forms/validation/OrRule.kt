package io.peekandpoke.kraft.forms.validation

import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.joinOr
import io.peekandpoke.ultra.i18n.I18nTranslate

/** Composite rule that passes if any of the contained [rules] passes. */
class OrRule<T>(private val rules: List<Rule<T>>) : Rule<T> {

    override suspend fun check(value: T): Boolean {
        return rules.any { it.check(value) }
    }

    override suspend fun getMessage(value: T, translate: I18nTranslate?): String {
        return rules.map { it.getMessage(value, translate) }
            .filter { it.isNotBlank() }
            .joinToString(translate?.forms?.joinOr() ?: " or ")
    }
}
