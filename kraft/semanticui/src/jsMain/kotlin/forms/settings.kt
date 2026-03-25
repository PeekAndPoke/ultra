package io.peekandpoke.kraft.semanticui.forms

import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.SemanticFn
import io.peekandpoke.ultra.semanticui.SemanticIconFn
import io.peekandpoke.ultra.semanticui.SemanticTag
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.semantic
import kotlinx.html.DIV
import kotlinx.html.I
import kotlinx.html.InputType

interface SemanticOptions<T> : io.peekandpoke.kraft.forms.FieldOptions<T> {

    companion object {
        val appearKey = TypedKey<SemanticFn>("appear")
    }

    interface Checkbox<T> : io.peekandpoke.kraft.forms.CheckboxOptions<T> {

        companion object {
            val styleKey = TypedKey<SemanticFn>("style")
        }

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        val style get() = access(styleKey)

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun toggle() {
            style { toggle }
        }

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun slider() {
            style { slider }
        }
    }

    interface Input<T> : io.peekandpoke.kraft.forms.InputOptions<T> {
        companion object {
            val wrapFieldWithKey = TypedKey<SemanticFn>("wrap-field-with")
            val beforeFieldKey = TypedKey<DIV.(UiInputFieldComponent<*, *>) -> Unit>("before-field")
            val afterFieldKey = TypedKey<DIV.(UiInputFieldComponent<*, *>) -> Unit>("after-field")
        }

        @JsName("rightIcon")
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightIcon(iconFn: SemanticIconFn) {
            rightIcon(iconFn = iconFn, block = {})
        }

        @JsName("rightIconWithBlock")
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
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
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
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
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
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
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun leftIcon(iconFn: SemanticIconFn) {
            attributes[wrapFieldWithKey] = semantic { left.icon.input }

            val fn: DIV.(UiInputFieldComponent<*, *>) -> Unit = { icon.iconFn().invoke() }
            attributes[afterFieldKey] = fn
        }

        @JsName("rightLabel")
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun rightLabel(labelFn: DIV.(UiInputFieldComponent<T, *>) -> Unit) {
            wrapFieldWith { right.labeled.input }

            @Suppress("UNCHECKED_CAST")
            attributes[afterFieldKey] = labelFn as DIV.(UiInputFieldComponent<*, *>) -> Unit
        }

        @JsName("leftLabel")
        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun leftLabel(labelFn: DIV.(UiInputFieldComponent<T, *>) -> Unit) {
            wrapFieldWith { left.labeled.input }

            @Suppress("UNCHECKED_CAST")
            attributes[beforeFieldKey] = labelFn as DIV.(UiInputFieldComponent<*, *>) -> Unit
        }

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun wrapFieldWith(): (SemanticTag.() -> SemanticTag)? = attributes[wrapFieldWithKey]

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun wrapFieldWith(wrap: (SemanticTag.() -> SemanticTag)) {
            val current = wrapFieldWith()

            attributes[wrapFieldWithKey] = semantic {
                current?.invoke(this)
                wrap()
            }
        }

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun renderBeforeField(): (DIV.(UiInputFieldComponent<*, *>) -> Unit)? = attributes[beforeFieldKey]

        @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
        fun renderAfterField(): (DIV.(UiInputFieldComponent<*, *>) -> Unit)? = attributes[afterFieldKey]
    }

    @io.peekandpoke.kraft.forms.KraftFormsSettingDsl
    val appear get() = access(appearKey)
}
