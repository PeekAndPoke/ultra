package io.peekandpoke.kraft.semanticui.forms.old.misc

import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.forms.FormFieldComponent
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.semanticui.forms.renderErrors
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.div

/**
 * A form field component that has no user input element.
 *
 * Useful for displaying validation errors on computed or derived values that are not directly editable.
 */
open class NoInputFieldComponent<T>(ctx: Ctx<Props<T>>) :
    FormFieldComponent<T, NoInputFieldComponent.Props<T>>(ctx) {

    /** Props for [NoInputFieldComponent]. */
    data class Props<P>(
        val config: Config<P>,
    ) : FormFieldComponent.Props<P> {
        override val initialValue: P = config.value
        override val fromStr: (String) -> P get() = { initialValue }
        override val onChange: (P) -> Unit get() = {}
        override val rules: List<Rule<P>> get() = config.rules
        val render: VDom.(field: NoInputFieldComponent<P>) -> Unit = config.render
    }

    /** Configuration DSL for [NoInputFieldComponent], including validation rules and custom rendering. */
    class Config<T>(
        var value: T,
    ) {
        val asProps get() = Props(config = this)

        /** The validation rules */
        val rules: MutableList<Rule<T>> = mutableListOf()

        /** The rendering function */
        var render: VDom.(field: NoInputFieldComponent<T>) -> Unit = { field ->
            ui.given(field.hasErrors) { error }.field {
                renderErrors(field, this)
            }
        }
            private set

        /** Adds a validation rule */
        fun accepts(
            rule: Rule<T>,
            vararg rules: Rule<T>,
        ) = apply {
            this.rules.add(rule)
            this.rules.addAll(rules)
        }

        /** Set the rendering function */
        fun render(block: VDom.(field: NoInputFieldComponent<T>) -> Unit) {
            render = block
        }
    }

    init {
        lifecycle {
            onNextProps { new, old ->
                if (new.initialValue != old.initialValue) {
                    validate()
                }
            }
        }
    }

    override fun VDom.render() {
        div {
            key = autoDomKey

            props.render(this@render, this@NoInputFieldComponent)
        }
    }
}
