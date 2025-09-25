package de.peekandpoke.kraft.addons.semanticui.forms.old.misc

import de.peekandpoke.kraft.addons.semanticui.forms.renderErrors
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.div

open class NoInputFieldComponent<T>(ctx: Ctx<Props<T>>) :
    de.peekandpoke.kraft.forms.FormFieldComponent<T, NoInputFieldComponent.Props<T>>(ctx) {

    data class Props<P>(
        val config: Config<P>,
    ) : de.peekandpoke.kraft.forms.FormFieldComponent.Props<P> {
        override val initialValue: P = config.value
        override val fromStr: (String) -> P get() = { initialValue }
        override val onChange: (P) -> Unit get() = {}
        override val rules: List<de.peekandpoke.kraft.forms.validation.Rule<P>> get() = config.rules
        val render: VDom.(field: NoInputFieldComponent<P>) -> Unit = config.render
    }

    class Config<T>(
        var value: T,
    ) {
        val asProps get() = Props(config = this)

        /** The validation rules */
        val rules: MutableList<de.peekandpoke.kraft.forms.validation.Rule<T>> = mutableListOf()

        /** The rendering function */
        var render: VDom.(field: NoInputFieldComponent<T>) -> Unit = { field ->
            ui.given(field.hasErrors) { error }.field {
                renderErrors(field, this)
            }
        }
            private set

        /** Adds a validation rule */
        fun accepts(
            rule: de.peekandpoke.kraft.forms.validation.Rule<T>,
            vararg rules: de.peekandpoke.kraft.forms.validation.Rule<T>,
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
