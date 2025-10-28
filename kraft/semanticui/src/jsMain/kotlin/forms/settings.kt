package de.peekandpoke.kraft.semanticui.forms

import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.SemanticFn
import de.peekandpoke.ultra.semanticui.SemanticIconFn
import de.peekandpoke.ultra.semanticui.SemanticTag
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.semantic
import kotlinx.html.DIV
import kotlinx.html.I
import kotlinx.html.InputType

interface SemanticOptions<T> : de.peekandpoke.kraft.forms.FieldOptions<T> {

    companion object {
        val appearKey = TypedKey<SemanticFn>("appear")
    }

    interface Checkbox<T> : de.peekandpoke.kraft.forms.CheckboxOptions<T> {

        companion object {
            val styleKey = TypedKey<SemanticFn>("style")
        }

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        val style get() = access(styleKey)

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun toggle() {
            style { toggle }
        }

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun slider() {
            style { slider }
        }
    }

    interface Input<T> : de.peekandpoke.kraft.forms.InputOptions<T> {
        companion object {
            val wrapFieldWithKey = TypedKey<SemanticFn>("wrap-field-with")
            val beforeFieldKey = TypedKey<DIV.(UiInputFieldComponent<*, *>) -> Unit>("before-field")
            val afterFieldKey = TypedKey<DIV.(UiInputFieldComponent<*, *>) -> Unit>("after-field")
        }

        @JsName("rightIcon")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightIcon(iconFn: SemanticIconFn) {
            rightIcon(iconFn = iconFn, block = {})
        }

        @JsName("rightIconWithBlock")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightIcon(iconFn: SemanticIconFn, block: I.(UiInputFieldComponent<T, *>) -> Unit) {
            attributes[wrapFieldWithKey] = semantic { right.icon.input }

            val fn: DIV.(UiInputFieldComponent<T, *>) -> Unit = { field ->
                icon.iconFn().invoke {
                    block(field)
                }
            }
            @Suppress("UNCHECKED_CAST")
            attributes[afterFieldKey] = fn as DIV.(UiInputFieldComponent<*, *>) -> Unit
        }

        @JsName("clearingRightIcon")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightClearingIcon(iconFn: SemanticIconFn = { grey.times }) {
            attributes[wrapFieldWithKey] = semantic { right.icon.input }

            val fn: DIV.(UiInputFieldComponent<*, *>) -> Unit = {
                icon.link.iconFn().invoke {
                    onClick { _ ->
                        it.setInput("")
                        it.focus("input")
                    }
                }
            }

            attributes[afterFieldKey] = fn
        }

        @JsName("revealRevealPasswordIcon")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun revealPasswordIcon(
            hiddenIcon: SemanticIconFn = { eye_outline },
            visibleIcon: SemanticIconFn = { eye_slash_outline },
        ) {
            attributes[wrapFieldWithKey] = semantic { right.icon.input }

            val fn: DIV.(UiInputFieldComponent<*, *>) -> Unit = {

                val type = it.effectiveType ?: InputType.password

                val iconFn = when (type) {
                    InputType.password -> hiddenIcon
                    else -> visibleIcon
                }

                icon.link.iconFn().invoke {
                    onClick { _ ->
                        it.typeOverride = when (type) {
                            InputType.password -> InputType.text
                            else -> InputType.password
                        }
                    }
                }
            }

            attributes[afterFieldKey] = fn
        }

        @JsName("leftIcon")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun leftIcon(iconFn: SemanticIconFn) {
            attributes[wrapFieldWithKey] = semantic { left.icon.input }

            val fn: DIV.(UiInputFieldComponent<*, *>) -> Unit = { icon.iconFn().invoke() }
            attributes[afterFieldKey] = fn
        }

        @JsName("rightLabel")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightLabel(labelFn: DIV.(UiInputFieldComponent<T, *>) -> Unit) {
            wrapFieldWith { right.labeled.input }

            @Suppress("UNCHECKED_CAST")
            attributes[afterFieldKey] = labelFn as DIV.(UiInputFieldComponent<*, *>) -> Unit
        }

        @JsName("leftLabel")
        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun leftLabel(labelFn: DIV.(UiInputFieldComponent<T, *>) -> Unit) {
            wrapFieldWith { left.labeled.input }

            @Suppress("UNCHECKED_CAST")
            attributes[beforeFieldKey] = labelFn as DIV.(UiInputFieldComponent<*, *>) -> Unit
        }

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun wrapFieldWith(): (SemanticTag.() -> SemanticTag)? = attributes[wrapFieldWithKey]

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun wrapFieldWith(wrap: (SemanticTag.() -> SemanticTag)) {
            val current = wrapFieldWith()

            attributes[wrapFieldWithKey] = semantic {
                current?.invoke(this)
                wrap()
            }
        }

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun renderBeforeField(): (DIV.(UiInputFieldComponent<*, *>) -> Unit)? = attributes[beforeFieldKey]

        @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun renderAfterField(): (DIV.(UiInputFieldComponent<*, *>) -> Unit)? = attributes[afterFieldKey]
    }

    @de.peekandpoke.kraft.forms.KraftFormsSettingDsl
    val appear get() = access(appearKey)
}
