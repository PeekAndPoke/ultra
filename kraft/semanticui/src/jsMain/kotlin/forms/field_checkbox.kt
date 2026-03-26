package io.peekandpoke.kraft.semanticui.forms

import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.semanticui.forms.UiCheckBoxComponent.Options
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.key
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.document
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.input
import org.w3c.dom.HTMLInputElement
import kotlin.reflect.KMutableProperty0

/** Provides a [UiCheckboxFieldRenderer] for rendering checkbox form fields. */
@io.peekandpoke.kraft.forms.KraftFormsDsl
val Tag.UiCheckboxField get() = UiCheckboxFieldRenderer(this)

/**
 * Generic checkbox field factory that renders a [UiCheckBoxComponent].
 *
 * @param value The current value.
 * @param onChange Callback invoked when the checkbox is toggled.
 * @param off The value representing the unchecked state.
 * @param on The value representing the checked state.
 * @param builder Optional configuration for field options.
 */
@Suppress("FunctionName")
@io.peekandpoke.kraft.forms.KraftFormsDsl
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

/**
 * Semantic UI checkbox field component with on/off value mapping and validation.
 *
 * Wraps an HTML `<input type="checkbox">` and integrates with the Kraft forms system.
 */
class UiCheckBoxComponent<T, P : UiCheckBoxComponent.Props<T>>(ctx: Ctx<P>) :
    io.peekandpoke.kraft.forms.AbstractFormField<T, Options<T>, P>(ctx) {

    /** Configuration options for [UiCheckBoxComponent]. */
    class Options<T> : io.peekandpoke.kraft.forms.FieldOptions.Base<T>(), SemanticOptions<T>,
        SemanticOptions.Checkbox<T>

    /** Props for [UiCheckBoxComponent]. */
    data class Props<X>(
        override val value: X,
        override val onChange: (X) -> Unit,
        override val options: Options<X>,
        val on: X,
        val off: X,
    ) : io.peekandpoke.kraft.forms.AbstractFormField.Props<X, Options<X>>

    /** The underlying HTML input element. */
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
            key = options.domKey.getOrDefault(autoDomKey)

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

/** DSL renderer for [UiCheckBoxComponent], providing overloads for Boolean and generic types. */
class UiCheckboxFieldRenderer(private val tag: Tag) {
    /**
     * Renders the field for a Boolean
     */
    @io.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun invoke(
        prop: KMutableProperty0<Boolean>,
        builder: Options<Boolean>.() -> Unit = {},
    ) = invoke(prop(), prop::set, builder)

    /**
     * Renders the field for a Boolean
     */
    @io.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun invoke(
        value: Boolean,
        onChange: (Boolean) -> Unit,
        builder: Options<Boolean>.() -> Unit = {},
    ) = invoke(value = value, onChange = onChange, off = false, on = true, builder = builder)

    /**
     * Renders the field for the type [T]
     */
    @io.peekandpoke.kraft.forms.KraftFormsDsl
    operator fun <T> invoke(
        value: T,
        onChange: (T) -> Unit,
        on: T,
        off: T,
        builder: Options<T>.() -> Unit = {},
    ) = tag.UiCheckboxField(value = value, onChange = onChange, off = off, on = on, builder = builder)
}
