package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.i18n.generated.forms
import io.peekandpoke.kraft.i18n.generated.invalidValue
import io.peekandpoke.kraft.i18n.i18nCtrl
import io.peekandpoke.kraft.messages.sendMessage
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.ultra.i18n.I18nTranslate

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

    /** Current translations; re-validates on language switch so error messages update. */
    private val translate: I18nTranslate by subscribingTo(i18nCtrl.translateStream) {
        if (touched) launch { validate() }
    }

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

            errors = listOf(translate.forms.invalidValue())
        }

        sendMessage(FormFieldInputChanged(this))
    }

    /** Sets the value directly, touches the field, validates, and notifies via [Props.onChange]. */
    fun setValue(value: T) {
        touch()

        inputValue = value

        // Propagate synchronously (controlled inputs must not lag a dispatch behind each keystroke);
        // validate asynchronously since rules may suspend. Mirrors AbstractFormField.setValue.
        props.onChange(currentValue)

        launch { validate() }
    }

    override fun touch() {
        touched = true
    }

    override fun untouch() {
        touched = false
    }

    /** Monotonic token so out-of-order async validations (later keystroke) win over stale ones. */
    private var validationSeq = 0

    override suspend fun validate(): Boolean {
        if (!touched) {
            return errors.isEmpty()
        }

        val seq = ++validationSeq
        // Snapshot the value/translation once — async rules suspend, and currentValue may change
        // mid-flight; re-reading it would compute errors against a mix of values.
        val value = currentValue
        val t = translate

        val newErrors = props.rules
            .filter { !it.check(value) }
            .map { it.getMessage(value, t) }

        // Latest-wins: only publish if a newer validation hasn't started while we were suspended.
        if (seq == validationSeq) {
            errors = newErrors
        }

        return newErrors.isEmpty()
    }

    ////  RENDERING  ////////////////////////////////////////////////////////////////////////////////////////////////
}
