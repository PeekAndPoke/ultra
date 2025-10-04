package de.peekandpoke.kraft.semanticui.forms

import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.semanticui.forms.UiTextAreaComponent.Options
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onInput
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.css.Overflow
import kotlinx.css.maxHeight
import kotlinx.css.overflowY
import kotlinx.css.vh
import kotlinx.html.TEXTAREA
import kotlinx.html.Tag
import kotlinx.html.textArea
import org.w3c.dom.HTMLTextAreaElement

@Suppress("FunctionName")
@de.peekandpoke.kraft.forms.KraftFormsDsl
fun Tag.UiTextArea(
    value: String,
    onChange: (String) -> Unit,
    builder: Options.() -> Unit = {},
) = comp(
    UiTextAreaComponent.Props(
        value = value,
        onChange = onChange,
        options = Options().apply(builder),
    )
) {
    UiTextAreaComponent(it)
}

class UiTextAreaComponent(ctx: Ctx<Props>) :
    de.peekandpoke.kraft.forms.GenericFormField<String, Options, UiTextAreaComponent.Props>(ctx) {

    class Options : de.peekandpoke.kraft.forms.FieldOptions.Base<String>(),
        de.peekandpoke.kraft.forms.TextAreaOptions<String>, SemanticOptions<String>

    data class Props(
        override val value: String,
        override val onChange: (String) -> Unit,
        override val options: Options,
    ) : de.peekandpoke.kraft.forms.GenericFormField.Props<String, Options>

    val inputElement: HTMLTextAreaElement get() = dom!!.querySelector("textarea") as HTMLTextAreaElement

    private val isVerticalAutoResize get() = options.verticalAutoResize.getOrDefault(true)

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

    /**
     * Replaces the current selection with given [text].
     *
     * When no text is selected the [text] will be inserted at the cursor position.
     */
    fun replaceSelection(text: String) {
        val field = (dom?.querySelector("textarea") as? HTMLTextAreaElement)
            ?: return

        val startPos = field.selectionStart ?: 0
        val endPos = field.selectionEnd ?: startPos

        setValue(
            field.value.replaceRange(startPos, endPos, text)
        )
    }

    override fun VDom.render() {
        ui.with(options.appear.getOrDefault { this }).given(hasErrors) { error }.field {
            key = autoDomKey

            renderLabel("textarea")

            textArea {
                css {
                    maxHeight = 50.vh
                    overflowY = Overflow.auto
                }

                applyAll()

                // We need this otherwise the value might not update properly
                attributes["value"] = currentValue

                +currentValue

                options.customize()?.let { it() }
            }

            renderErrors(this)
        }

        // Apply automatic vertical resize
        if (isVerticalAutoResize) {
            window.requestAnimationFrame {
                val textarea = dom?.querySelector("textarea") as? HTMLTextAreaElement

                textarea?.let { t ->
                    t.style.height = "1px"
                    t.style.height = "${t.scrollHeight + 2}px"
                }
            }
        }
    }

    fun TEXTAREA.applyAll() {
        track()

        applyDisabled()
        applyName()
        applyPlaceholder()
    }

    fun TEXTAREA.track() {
        onInput {
            val target = it.target as HTMLTextAreaElement

            setValue(target.value)
        }
    }

    private fun TEXTAREA.applyDisabled() {
        if (options.isDisabled) {
            disabled = true
        }
    }

    private fun TEXTAREA.applyName() {
        options.name()?.let { name = it }
    }

    private fun TEXTAREA.applyPlaceholder() {
        options.placeholder()?.takeIf { it.isNotBlank() }?.let {
            placeholder = it
        }
    }
}
