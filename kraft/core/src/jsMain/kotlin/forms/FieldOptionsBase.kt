package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.forms.validation.Rule
import io.peekandpoke.kraft.utils.ResponsiveController
import io.peekandpoke.ultra.common.MutableTypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.html.RenderFunc
import kotlinx.html.InputType
import kotlinx.html.LABEL
import kotlinx.html.TEXTAREA

/** Builder lambda for configuring [FieldOptions]. */
typealias SettingsBuilder<T> = FieldOptions<T>.() -> Unit

/** Provides typed attribute access for field options. */
interface FieldOptionsAccess<T> {
    /** Returns a typed accessor for the given [key]. */
    fun <X> access(key: TypedKey<X>): FieldOptions.Access<T, X>
}

/**
 * Configuration for a form field: label, placeholder, disabled state, validation rules, and custom attributes.
 */
interface FieldOptions<T> : FieldOptionsAccess<T> {
    companion object {
        operator fun <T> invoke(): FieldOptions<T> = Base()

        private val domKeyKey = TypedKey<String>("domKey")
        private val labelKey = TypedKey<RenderFunc<LABEL>>("label")
        private val placeholderKey = TypedKey<String>("placeholder")
        private val nameKey = TypedKey<String>("name")
        private val requiredKey = TypedKey<Boolean>("required")
        private val disabledKey = TypedKey<Boolean>("disabled")
    }

    /** Default base implementation of [FieldOptions]. */
    open class Base<T> : FieldOptions<T> {

        override val attributes: MutableTypedAttributes = MutableTypedAttributes.empty()

        override val rules: MutableList<Rule<T>> = mutableListOf()

        /** Adds a validation rule */
        override fun accepts(vararg rules: Rule<T>) {
            this.rules.addAll(rules)
        }

        override fun label(label: String) {
            label { +label }
        }
    }

    /** Typed getter/setter for a single attribute on [FieldOptions]. */
    class Access<T, X>(private val settings: FieldOptions<T>, private val key: TypedKey<X>) {

        operator fun invoke(value: X) {
            settings.set(key, value)
        }

        operator fun invoke(): X? {
            return settings.get(key)
        }

        fun getOrDefault(default: X): X {
            return invoke() ?: default
        }

        fun getOrPut(produce: () -> X): X {
            return settings.getOrPut(key, produce)
        }
    }

    /** Mutable typed attributes storage backing all field options. */
    val attributes: MutableTypedAttributes

    /** Validation rules applied to this field. */
    val rules: List<Rule<T>>

    /** Access to the DOM key used for virtual DOM diffing. */
    val domKey get() = access(domKeyKey)

    /** Access to the field label renderer. */
    val label get() = access(labelKey)

    /** Access to the placeholder text. */
    val placeholder get() = access(placeholderKey)

    /** Access to the HTML name attribute. */
    val name get() = access(nameKey)

    /** Access to the required flag. */
    val required get() = access(requiredKey)

    /** Whether this field is currently disabled. */
    val isDisabled: Boolean get() = access(disabledKey).invoke() ?: false

    /** Adds validation rules to this field. */
    fun accepts(vararg rules: Rule<T>)

    /** Sets a plain text label for this field. */
    fun label(label: String)

    /** Disables or enables this field. */
    fun disabled(disabled: Boolean = true) {
        access(disabledKey)(disabled)
    }

    private fun <X> set(key: TypedKey<X>, value: X) {
        attributes[key] = value
    }

    private fun <X> get(key: TypedKey<X>): X? {
        return attributes[key]
    }

    private fun <X> getOrPut(key: TypedKey<X>, produce: () -> X): X {
        return attributes.getOrPut(key, produce)
    }

    override fun <X> access(key: TypedKey<X>): Access<T, X> = Access(this, key)
}

/** Mixin providing autofocus configuration for form fields. */
interface AutofocusOptions<T> : FieldOptionsAccess<T> {
    companion object {
        private val autofocusKey = TypedKey<Boolean>("autofocus")
    }

    val autofocusValue: FieldOptions.Access<T, Boolean>
        get() = access(
            autofocusKey
        )

    fun autofocus(focus: Boolean = true) {
        autofocusValue(focus)
    }

    fun autofocusOnDesktop(responsive: ResponsiveController.State) {
        autofocus(responsive.isDesktop)
    }
}

/** Options for checkbox form fields. */
interface CheckboxOptions<T> : FieldOptions<T>,
    AutofocusOptions<T>

/** Options for input form fields, adding step, type, and format value configuration. */
interface InputOptions<T> : FieldOptions<T>, AutofocusOptions<T> {
    companion object {
        private val stepKey = TypedKey<Number>("step")
        private val typeKey = TypedKey<InputType>("type")
        private val formatValueKey = TypedKey<String>("format-value")
    }

    val formatValue get() = access(formatValueKey)

    val step get() = access(stepKey)

    val type get() = access(typeKey)

    fun asDateInput() {
        type(InputType.date)
        formatValue("yyyy-MM-dd")
    }

    fun asDateTimeInput() {
        type(InputType.dateTimeLocal)
        formatValue("yyyy-MM-ddTHH:mm:ss")
    }

    fun asTimeInput() {
        type(InputType.time)
        formatValue("HH:mm:ss")
    }
}

/** Options for textarea form fields, adding vertical auto-resize and customization. */
interface TextAreaOptions<T> : FieldOptions<T>,
    AutofocusOptions<T> {

    companion object {
        private val verticalAutoResizeKey = TypedKey<Boolean>("verticalAutoResize")
        private val customizeKey = TypedKey<RenderFunc<TEXTAREA>>("customize")
    }

    val verticalAutoResize get() = access(verticalAutoResizeKey)

    val customize get() = access(customizeKey)
}
