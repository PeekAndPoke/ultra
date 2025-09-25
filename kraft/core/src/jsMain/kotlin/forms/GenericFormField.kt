package de.peekandpoke.kraft.forms

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.messages.sendMessage
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.noui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent

@Suppress("FunctionName")
fun <T> Tag.GenericFormField(
    value: T,
    onChange: (T) -> Unit,
    options: FieldOptions<T>,
) = comp(
    GenericFormField.DefaultProps(
        value = value,
        onChange = onChange,
        options = options,
    )
) {
    GenericFormField(it)
}

open class GenericFormField<T, O : FieldOptions<T>, P : GenericFormField.Props<T, O>>(
    ctx: Ctx<P>,
) : Component<P>(ctx), FormField<T> {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    interface Props<T, O : FieldOptions<T>> {
        val value: T
        val onChange: (T) -> Unit
        val options: O
    }

    data class DefaultProps<T, O : FieldOptions<T>>(
        override val value: T,
        override val onChange: (T) -> Unit,
        override val options: O,
    ) : Props<T, O>

    val options: O get() = props.options

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Track if the input value was modified.
     *
     * 'True' means the value was modified and thus the field was touched.
     * 'False' means the value was not modified yet.
     */
    override var touched: Boolean by value(false)

    /**
     * A list of validation errors.
     */
    override var errors: List<String> by value(emptyList())

    /**
     * The input value set by the user.
     */
    private var _value: T by value(props.value)

    /**
     * The effective value
     *
     * - it is either a value set by the user (see [setValue])
     * - or the initial value coming through the props (see [Props.value])
     */
    val currentValue: T get() = _value

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                sendMessage(FormFieldMountedMessage(this@GenericFormField))
            }

            onUnmount {
                sendMessage(FormFieldUnmountedMessage(this@GenericFormField))
            }

            onNextProps { new, _ ->
                _value = new.value
            }
        }
    }

    override fun shouldRedraw(nextProps: P): Boolean {
        return props.value != nextProps.value ||
                props.options.attributes.entries.size != nextProps.options.attributes.entries.size ||
                // TODO: improve this as this is not very efficient
                props.options.attributes.asImmutable() != nextProps.options.attributes.asImmutable()
    }

    override fun reset() {
        touched = false
        _value = props.value
        errors = emptyList()
    }

    override fun touch() {
        touched = true
    }

    override fun untouch() {
        touched = false
    }

    override fun validate(): Boolean {

        if (touched) {
            errors = props.options.rules
                .filter { !it.check(currentValue) }
                .map { it.getMessage(currentValue) }
        }

        return errors.isEmpty()
    }

    override fun VDom.render() {
    }

    fun focus(cssSelector: String) {
        (dom?.querySelector(cssSelector) as? HTMLElement)?.focus()
    }

    fun setValue(value: T) {
//        console.log("setValue", value, (value as Any)::class)

        if (this._value != value) {
            try {
                sendMessage(_root_ide_package_.de.peekandpoke.kraft.forms.FormFieldInputChanged(this))
            } finally {
            }

            touch()
            this._value = value
            props.onChange(value)
        }

        validate()
    }

    // Label Helpers

    fun FlowContent.renderLabel(focusCssSelector: String? = null) {

        renderLabel(
            onClick = focusCssSelector?.let { sel ->
                { focus(sel) }
            }
        )
    }

    fun FlowContent.renderLabel(onClick: ((evt: MouseEvent) -> Unit)? = null) {
        options.label()?.let { l ->
            noui Label {
                l()

                onClick?.let { on ->
                    onClick { evt ->
                        on(evt)
                    }
                }
            }
        }
    }
}
