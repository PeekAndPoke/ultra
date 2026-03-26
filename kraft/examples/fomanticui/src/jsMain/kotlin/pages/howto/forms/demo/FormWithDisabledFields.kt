@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.CodePanelColumn
import io.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import io.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.semanticui.forms.UiCheckboxField
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.semanticui.forms.old.select.SelectField
import io.peekandpoke.kraft.semanticui.forms.old.select.SelectFieldComponent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.flowOf
import kotlinx.html.InputType
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithDisabledFields() = comp {
    FormWithDisabledFields(it)
}

class FormWithDisabledFields(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val input: String = "This a disabled input field",
        val textarea: String = "This is a disabled textarea",
        val checkbox: Boolean = true,
        val select: String = "Select",
        val suggest: String = "Suggest",
        val color: String = "#FFFFFF",
    )

    private var state by value(State())
    private var draft by value(state)

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock disabledFields>
                    ui.three.fields {
                        UiCheckboxField(draft.checkbox, { draft = draft.copy(checkbox = it) }) {
                            label { +State::checkbox.name }
                            toggle()
                            disabled()
                        }

                        UiInputField(draft.input, { draft = draft.copy(input = it) }) {
                            label { +State::input.name }
                            disabled()
                        }

                        UiTextArea(draft.textarea, { draft = draft.copy(textarea = it) }) {
                            label { +State::textarea.name }
                            disabled()
                        }
                    }

                    ui.three.fields {
                        SelectField(draft.select, { draft = draft.copy(select = it) }) {
                            label { +State::select.name }
                            disabled()

                            option("Select", "Select")
                        }

                        SelectField(draft.suggest, { draft = draft.copy(suggest = it) }) {
                            label { +State::suggest.name }
                            disabled()

                            autoSuggest { _ ->
                                flowOf(
                                    listOf(
                                        SelectFieldComponent.Option("Suggest", "Suggest"),
                                        SelectFieldComponent.Option("A", "A"),
                                        SelectFieldComponent.Option("B", "B"),
                                        SelectFieldComponent.Option("C", "C"),
                                    )
                                )
                            }
                        }

                        UiInputField(draft.color, { draft = draft.copy(color = it) }) {
                            label { +State::color.name }
                            type(InputType.color)
                            disabled()
                        }
                    }
                    // </CodeBlock>
                }

                ui.divider {}

                val canSubmit = formCtrl.isValid && draft != state

                ui.blue.button.given(!canSubmit) { disabled }.then {
                    onClick {
                        if (formCtrl.validate() && canSubmit) {
                            state = draft
                        }
                    }
                    +"Submit"
                }

                ui.basic.button {
                    onClick {
                        draft = state
                        formCtrl.resetAllFields()
                    }
                    +"Reset form"
                }

                ui.divider {}

                renderStateAndDraftTable(
                    state,
                    draft,
                    listOf(
                        State::checkbox { it.toString() },
                        State::input { it },
                        State::textarea { it },
                        State::select { it },
                        State::suggest { it },
                    ),
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithDisabledFields_kt_disabledFields)
        }
    }
}
