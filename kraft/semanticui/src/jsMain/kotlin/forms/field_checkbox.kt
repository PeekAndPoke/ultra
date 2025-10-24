package de.peekandpoke.kraft.semanticui.forms

import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.forms.UiCheckBoxComponent.Options
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.key
import de.peekandpoke.ultra.html.onChange
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.document
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.input
import org.w3c.dom.HTMLInputElement
import kotlin.reflect.KMutableProperty0

@de.peekandpoke.kraft.forms.KraftFormsDsl
val Tag.UiCheckboxField get() = UiCheckboxFieldRenderer(this)

@Suppress("FunctionName")
@de.peekandpoke.kraft.forms.KraftFormsDsl
fun <T> Tag.UiCheckboxField(
    value: T,
    onChange: (T) -> Unit,
    off: T,
    on: T,
    builder: Options<T>.() -> Unit = {},
) = comp(
    UiCheckBoxComponent.Props(
        value = value,
        onChange = onChange,
        off = off,
        on = on,
        options = Options<T>().apply(builder),
    )
) {
    UiCheckBoxComponent(it)
}

class UiCheckBoxComponent<T, P : UiCheckBoxComponent.Props<T>>(ctx: Ctx<P>) :
    de.peekandpoke.kraft.forms.GenericFormField<T, Options<T>, P>(ctx) {

    class Options<T> : de.peekandpoke.kraft.forms.FieldOptions.Base<T>(), SemanticOptions<T>,
        SemanticOptions.Checkbox<T>

    data class Props<X>(
        override val value: X,
        override val onChange: (X) -> Unit,
        override val options: Options<X>,
        val on: X,
        val off: X,
    ) : de.peekandpoke.kraft.forms.GenericFormField.Props<X, Options<X>>

    val inputElement: HTMLInputElement get() = dom!!.querySelector("input") as HTMLInputElement

    init {
        lifecycle {
            onMount {
                props.options.autofocusValue()?.takeIf { it }?.let {
                    focus()
                }
            }
        }
    }

    /**
     * Sets the focus on the input element
     */
    fun focus() {
        inputElement.focus()
    }

    /**
     * Returns true when the text area has the focus.
     */
    fun hasFocus(): Boolean {
        return document.activeElement === inputElement
    }


    override fun VDom.render() {

        ui.with(options.appear.getOrDefault { this }).given(hasErrors) { error }.field {
            key = autoDomKey

            div {
                ui.with(options.style.getOrDefault { this }).checkbox {
                    input {
                        onChange {
                            when ((it.target as HTMLInputElement).checked) {
                                true -> setValue(props.on)
                                false -> setValue(props.off)
                            }
                        }
                        type = InputType.checkBox
                        checked = currentValue == props.on

                        applyDisabled()
                        applyName()
                    }

                    renderLabel {
                        setValue(
                            when (currentValue) {
                                props.on -> props.off
                                else -> props.on
                            }
                        )
                    }
                }
            }

            renderErrors(this)
        }
    }

    private fun INPUT.applyDisabled() {
        if (options.isDisabled) {
            disabled = true
        }
    }

    private fun INPUT.applyName() {
        options.name()?.let { name = it }
    }
}

class UiCheckboxFieldRenderer(private val tag: Tag) {
    /**
     * Renders the field for a Boolean
     */
    @de.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun invoke(
        prop: KMutableProperty0<Boolean>,
        builder: Options<Boolean>.() -> Unit = {},
    ) = invoke(prop(), prop::set, builder)

    /**
     * Renders the field for a Boolean
     */
    @de.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun invoke(
        value: Boolean,
        onChange: (Boolean) -> Unit,
        builder: Options<Boolean>.() -> Unit = {},
    ) = invoke(value = value, onChange = onChange, off = false, on = true, builder = builder)

    /**
     * Renders the field for the type [T]
     */
    @de.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun <T> invoke(
        value: T,
        onChange: (T) -> Unit,
        on: T,
        off: T,
        builder: Options<T>.() -> Unit = {},
    ) = tag.UiCheckboxField(value = value, onChange = onChange, off = off, on = on, builder = builder)
}
