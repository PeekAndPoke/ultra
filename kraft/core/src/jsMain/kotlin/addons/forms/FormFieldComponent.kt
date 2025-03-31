package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.addons.forms.validation.Rule
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.messages.sendMessage

abstract class FormFieldComponent<T, P : FormFieldComponent.Props<T>>(
    ctx: Ctx<P>,
) : FormField<P>, Component<P>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

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
