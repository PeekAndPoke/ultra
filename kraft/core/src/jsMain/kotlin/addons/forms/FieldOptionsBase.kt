package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.addons.forms.validation.Rule
import de.peekandpoke.kraft.semanticui.RenderFunc
import de.peekandpoke.kraft.utils.ResponsiveController
import de.peekandpoke.ultra.common.MutableTypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import kotlinx.html.InputType
import kotlinx.html.LABEL
import kotlinx.html.TEXTAREA

typealias SettingsBuilder<T> = FieldOptions<T>.() -> Unit

interface FieldOptionsAccess<T> {
    fun <X> access(key: TypedKey<X>): FieldOptions.Access<T, X>
}

interface FieldOptions<T> : FieldOptionsAccess<T> {
    companion object {
        operator fun <T> invoke(): FieldOptions<T> = Base()

        private val labelKey = TypedKey<RenderFunc<LABEL>>("label")
        private val placeholderKey = TypedKey<String>("placeholder")
        private val nameKey = TypedKey<String>("name")
        private val requiredKey = TypedKey<Boolean>("required")
        private val disabledKey = TypedKey<Boolean>("disabled")
    }

    open class Base<T> : FieldOptions<T> {

        override val attributes: MutableTypedAttributes = MutableTypedAttributes.empty()

        @KraftFormsSettingDsl
        override val rules: MutableList<Rule<T>> = mutableListOf()

        /** Adds a validation rule */
        @KraftFormsSettingDsl
        override fun accepts(vararg rules: Rule<T>) {
            this.rules.addAll(rules)
        }

        @KraftFormsSettingDsl
        override fun label(label: String) {
            label { +label }
        }
    }

    class Access<T, X>(private val settings: FieldOptions<T>, private val key: TypedKey<X>) {

        @KraftFormsSettingDsl
        operator fun invoke(value: X) {
            settings.set(key, value)
        }

        @KraftFormsSettingDsl
        operator fun invoke(): X? {
            return settings.get(key)
        }

        @KraftFormsSettingDsl
        fun getOrDefault(default: X): X {
            return invoke() ?: default
        }

        @KraftFormsSettingDsl
        fun getOrPut(produce: () -> X): X {
            return settings.getOrPut(key, produce)
        }
    }

    val attributes: MutableTypedAttributes

    @KraftFormsSettingDsl
    val rules: List<Rule<T>>

    @KraftFormsSettingDsl
    val label get() = access(labelKey)

    @KraftFormsSettingDsl
    val placeholder get() = access(placeholderKey)

    @KraftFormsSettingDsl
    val name get() = access(nameKey)

    @KraftFormsSettingDsl
    val required get() = access(requiredKey)

    @KraftFormsSettingDsl
    val isDisabled: Boolean get() = access(disabledKey).invoke() ?: false

    /** Adds a validation rule */
    @KraftFormsSettingDsl
    fun accepts(vararg rules: Rule<T>)

    @KraftFormsSettingDsl
    fun label(label: String)

    @KraftFormsSettingDsl
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

interface AutofocusOptions<T> : FieldOptionsAccess<T> {
    companion object {
        private val autofocusKey = TypedKey<Boolean>("autofocus")
    }

    @KraftFormsSettingDsl
    val autofocusValue: FieldOptions.Access<T, Boolean> get() = access(autofocusKey)

    @KraftFormsSettingDsl
    fun autofocus(focus: Boolean = true) {
        autofocusValue(focus)
    }

    @KraftFormsSettingDsl
    fun autofocusOnDesktop(responsive: ResponsiveController.State) {
        autofocus(responsive.isDesktop)
    }
}

interface CheckboxOptions<T> : FieldOptions<T>, AutofocusOptions<T>

interface InputOptions<T> : FieldOptions<T>, AutofocusOptions<T> {
    companion object {
        private val stepKey = TypedKey<Number>("step")
        private val typeKey = TypedKey<InputType>("type")
        private val formatValueKey = TypedKey<String>("format-value")
    }

    @KraftFormsSettingDsl
    val formatValue get() = access(formatValueKey)

    @KraftFormsSettingDsl
    val step get() = access(stepKey)

    @KraftFormsSettingDsl
    val type get() = access(typeKey)

    @KraftFormsSettingDsl
    fun asDateInput() {
        type(InputType.date)
        formatValue("yyyy-MM-dd")
    }

    @KraftFormsSettingDsl
    fun asDateTimeInput() {
        type(InputType.dateTimeLocal)
        formatValue("yyyy-MM-ddTHH:mm:ss")
    }

    @KraftFormsSettingDsl
    fun asTimeInput() {
        type(InputType.time)
        formatValue("HH:mm:ss")
    }
}

interface TextAreaOptions<T> : FieldOptions<T>, AutofocusOptions<T> {

    companion object {
        private val verticalAutoResizeKey = TypedKey<Boolean>("verticalAutoResize")
        private val customizeKey = TypedKey<RenderFunc<TEXTAREA>>("customize")
    }

    @KraftFormsSettingDsl
    val verticalAutoResize get() = access(verticalAutoResizeKey)

    @KraftFormsSettingDsl
    val customize get() = access(customizeKey)
}
