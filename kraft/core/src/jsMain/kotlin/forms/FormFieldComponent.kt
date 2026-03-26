package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.messages.sendMessage

/**
 * Alternative form field base class that converts string input via [Props.fromStr].
 *
 * Suitable for fields where the raw DOM input is a string that needs parsing.
 */
abstract class FormFieldComponent<T, P : FormFieldComponent.Props<T>>(
    ctx: Ctx<P>,
) : FormField<P>, Component<P>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** Props providing the initial value, string parser, change callback, and validation rules. */
    interface Props<P> {
        val initialValue: P
        val fromStr: (String) -> P
        val onChange: (P) -> Unit
        val rules: List<Rule<P>>
    }

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    override var touched by value(false)

    override var errors by value<List<String>>(emptyList())

    private var inputValue: T? = null

    /** The effective value: user input if set, otherwise the initial value from props. */
    val currentValue
        get() = when (val input = inputValue) {
            null -> props.initialValue
            else -> input
        }

    ////  LIVE-CYCLE  /////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                sendMessage(FormFieldMountedMessage(this@FormFieldComponent))
            }

            onNextProps { new, previous ->
                // A changing input value overrides the current input
                if (new.initialValue != previous.initialValue) {
                    // Override the current user input
                    inputValue = null
                    // Notify the form controller about the change
                    sendMessage(FormFieldInputChanged(this@FormFieldComponent))
                }
            }

            onUnmount {
                sendMessage(FormFieldUnmountedMessage(this@FormFieldComponent))
            }
        }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun reset() {
        touched = false
        inputValue = null
        errors = emptyList()
    }

    /** Parses a string input via [Props.fromStr] and sets the resulting value. */
    fun setInput(input: String) {
        try {
            val newValue = props.fromStr(input)
            setValue(newValue)
        } catch (t: Throwable) {
            console.error(t)

            // TODO: how to translate this?
            errors = listOf("Invalid value")
        }

        sendMessage(FormFieldInputChanged(this))
    }

    /** Sets the value directly, touches the field, validates, and notifies via [Props.onChange]. */
    fun setValue(value: T) {
        touch()

        inputValue = value

        if (validate()) {
            props.onChange(currentValue)
        }
    }

    override fun touch() {
        touched = true
    }

    override fun untouch() {
        touched = false
    }

    override fun validate(): Boolean {
        if (touched) {
            errors = props.rules
                .filter { !it.check(currentValue) }
                .map { it.getMessage(currentValue) }
        }

        return errors.isEmpty()
    }

    ////  RENDERING  ////////////////////////////////////////////////////////////////////////////////////////////////
}
