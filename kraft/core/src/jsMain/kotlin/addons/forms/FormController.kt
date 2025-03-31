package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.messages.onMessage

open class FormController(private val component: Component<*>, val stopEvents: Boolean = true) {

    companion object {
        fun watcher(component: Component<*>) = FormController(component = component, stopEvents = false)
    }

    private val _fields = mutableSetOf<FormField<*>>()

    val fields get() = _fields

    /**
     * Returns `true` when all fields are valid
     */
    val isValid
        get(): Boolean {
            return numErrors == 0
        }

    /**
     * Returns `true` when some fields are NOT valid
     */
    val isNotValid get() = !isValid

    /**
     * Number of fields with errors
     */
    val numErrors
        get(): Int {
            return _fields.filter { !it.validate() }.size
        }

    /**
     * Resets the input of all fields
     */
    fun resetAllFields() {
        _fields.forEach { it.reset() }
    }

    /**
     * Validates all fields by touching them first.
     *
     * When a field is touched, it's errors will be displayed.
     */
    fun validate(): Boolean {

        val before = isValid

        _fields.forEach { it.touch() }

        val after = isValid

        if (before != after) {
            component.triggerRedraw()
        }

        return after
    }

    /**
     * Validates all fields by touching them first and showing potential errors.
     *
     * See [validate].
     *
     * When the form is valid [andThen] is executed.
     */
    fun ifValidate(andThen: () -> Unit) {
        if (validate()) {
            andThen()
        }
    }

    init {
        component.onMessage<FormFieldInputChanged<*>> {
            component.triggerRedraw()

            if (stopEvents) {
                it.stop()
            }
        }

        component.onMessage<FormFieldMountedMessage<*>> {
            _fields.add(it.field)

            if (stopEvents) {
                it.stop()
            }
        }

        component.onMessage<FormFieldUnmountedMessage<*>> {
            _fields.remove(it.sender)

            if (stopEvents) {
                it.stop()
            }
        }
    }
}
